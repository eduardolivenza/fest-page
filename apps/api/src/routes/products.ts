import type { FastifyPluginAsync } from 'fastify';

export const productsRoutes: FastifyPluginAsync = async (app) => {
  app.get('/', async (request) => {
    const { category, featured, page = '1', pageSize = '12' } = request.query as Record<string, string>;

    const take = Math.min(parseInt(pageSize, 10), 50);
    const skip = (parseInt(page, 10) - 1) * take;

    const where = {
      ...(category && { category: category as 'BED' | 'MATTRESS' | 'PILLOW' | 'ACCESSORY' }),
      ...(featured === 'true' && { featured: true }),
    };

    const [data, total] = await Promise.all([
      app.prisma.product.findMany({
        where,
        include: {
          images: { orderBy: { position: 'asc' }, take: 1 },
          sizes: { orderBy: [{ width: 'asc' }, { length: 'asc' }] },
        },
        orderBy: [{ featured: 'desc' }, { createdAt: 'desc' }],
        take,
        skip,
      }),
      app.prisma.product.count({ where }),
    ]);

    return {
      data: data.map((p) => ({
        ...p,
        price: Number(p.price),
        sizes: p.sizes.map((s) => ({ ...s, price: Number(s.price) })),
      })),
      total,
      page: parseInt(page, 10),
      pageSize: take,
      totalPages: Math.ceil(total / take),
    };
  });

  app.get('/:idOrSlug', async (request, reply) => {
    const { idOrSlug } = request.params as { idOrSlug: string };

    const product = await app.prisma.product.findFirst({
      where: { OR: [{ id: idOrSlug }, { slug: idOrSlug }] },
      include: {
        images: { orderBy: { position: 'asc' } },
        sizes: { orderBy: [{ width: 'asc' }, { length: 'asc' }] },
      },
    });

    if (!product) {
      return reply.status(404).send({ message: 'Product not found' });
    }

    return {
      ...product,
      price: Number(product.price),
      sizes: product.sizes.map((s) => ({ ...s, price: Number(s.price) })),
    };
  });
};
