import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';

const sizeSchema = z.object({
  width: z.number().int().positive(),
  length: z.number().int().positive(),
  price: z.number().positive(),
});

const productSchema = z.object({
  name: z.string().min(2).max(200),
  slug: z.string().regex(/^[a-z0-9-]+$/),
  description: z.string().optional(),
  price: z.number().positive(),
  category: z.enum(['BED', 'MATTRESS', 'PILLOW', 'ACCESSORY']),
  featured: z.boolean().optional(),
  sizes: z.array(sizeSchema).optional(),
});

const sizesInclude = { orderBy: [{ width: 'asc' as const }, { length: 'asc' as const }] };

function serializeProduct(p: { price: { toNumber: () => number } | number; sizes?: Array<{ price: { toNumber: () => number } | number }> } & Record<string, unknown>) {
  return {
    ...p,
    price: typeof p.price === 'number' ? p.price : (p.price as { toNumber: () => number }).toNumber(),
    sizes: p.sizes?.map((s) => ({
      ...s,
      price: typeof s.price === 'number' ? s.price : (s.price as { toNumber: () => number }).toNumber(),
    })),
  };
}

export const adminProductsRoutes: FastifyPluginAsync = async (app) => {
  app.addHook('preHandler', app.authenticate);

  app.get('/', async (request) => {
    const { page = '1', pageSize = '20' } = request.query as Record<string, string>;
    const take = Math.min(parseInt(pageSize, 10), 100);
    const skip = (parseInt(page, 10) - 1) * take;

    const [data, total] = await Promise.all([
      app.prisma.product.findMany({
        include: { images: { orderBy: { position: 'asc' } }, sizes: sizesInclude },
        orderBy: { createdAt: 'desc' },
        take,
        skip,
      }),
      app.prisma.product.count(),
    ]);

    return {
      data: data.map(serializeProduct),
      total,
      page: parseInt(page, 10),
      pageSize: take,
      totalPages: Math.ceil(total / take),
    };
  });

  app.get('/:id', async (request, reply) => {
    const { id } = request.params as { id: string };
    const product = await app.prisma.product.findUnique({
      where: { id },
      include: { images: { orderBy: { position: 'asc' } }, sizes: sizesInclude },
    });
    if (!product) return reply.status(404).send({ message: 'Not found' });
    return serializeProduct(product);
  });

  app.post('/', async (request, reply) => {
    const body = productSchema.safeParse(request.body);
    if (!body.success) return reply.status(400).send({ message: 'Invalid data', errors: body.error.flatten() });

    const { sizes, ...productData } = body.data;
    const product = await app.prisma.product.create({
      data: { ...productData, sizes: sizes ? { create: sizes } : undefined },
      include: { sizes: sizesInclude },
    });
    return reply.status(201).send(serializeProduct(product));
  });

  app.put('/:id', async (request, reply) => {
    const { id } = request.params as { id: string };
    const body = productSchema.partial().safeParse(request.body);
    if (!body.success) return reply.status(400).send({ message: 'Invalid data', errors: body.error.flatten() });

    const { sizes, ...productData } = body.data;
    const product = await app.prisma.$transaction(async (tx) => {
      if (sizes !== undefined) {
        await tx.productSize.deleteMany({ where: { productId: id } });
      }
      return tx.product.update({
        where: { id },
        data: { ...productData, sizes: sizes ? { create: sizes } : undefined },
        include: { sizes: sizesInclude },
      });
    });
    return serializeProduct(product);
  });

  app.delete('/:id', async (request, reply) => {
    const { id } = request.params as { id: string };
    await app.prisma.product.delete({ where: { id } });
    return reply.status(204).send();
  });
};
