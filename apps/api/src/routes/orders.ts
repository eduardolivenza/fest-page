import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';

const createOrderSchema = z.object({
  customerName: z.string().min(2).max(100),
  customerEmail: z.string().email(),
  customerPhone: z.string().optional(),
  items: z.array(
    z.object({ productId: z.string(), quantity: z.number().int().min(1) }),
  ).min(1),
  notes: z.string().max(500).optional(),
});

export const ordersRoutes: FastifyPluginAsync = async (app) => {
  app.post('/', async (request, reply) => {
    const body = createOrderSchema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({ message: 'Invalid data', errors: body.error.flatten() });
    }

    const { customerName, customerEmail, customerPhone, items, notes } = body.data;

    const productIds = items.map((i) => i.productId);
    const products = await app.prisma.product.findMany({
      where: { id: { in: productIds } },
    });

    if (products.length !== productIds.length) {
      return reply.status(400).send({ message: 'One or more products not found' });
    }

    const customer = await app.prisma.customer.upsert({
      where: { email: customerEmail },
      update: { name: customerName, phone: customerPhone ?? null },
      create: { name: customerName, email: customerEmail, phone: customerPhone ?? null },
    });

    const order = await app.prisma.order.create({
      data: {
        customerId: customer.id,
        notes,
        items: {
          create: items.map((item) => {
            const product = products.find((p) => p.id === item.productId)!;
            return {
              productId: item.productId,
              quantity: item.quantity,
              unitPrice: product.price,
            };
          }),
        },
      },
      include: { items: true },
    });

    return reply.status(201).send(order);
  });
};
