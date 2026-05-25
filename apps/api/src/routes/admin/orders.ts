import type { FastifyPluginAsync } from 'fastify';

export const adminOrdersRoutes: FastifyPluginAsync = async (app) => {
  app.addHook('preHandler', app.authenticate);

  app.get('/', async (request) => {
    const { status, page = '1', pageSize = '20' } = request.query as Record<string, string>;
    const take = Math.min(parseInt(pageSize, 10), 100);
    const skip = (parseInt(page, 10) - 1) * take;

    const where = status ? { status: status as 'PENDING' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED' } : {};

    const [data, total] = await Promise.all([
      app.prisma.order.findMany({
        where,
        include: {
          customer: true,
          items: { include: { product: { select: { id: true, name: true, price: true } } } },
        },
        orderBy: { createdAt: 'desc' },
        take,
        skip,
      }),
      app.prisma.order.count({ where }),
    ]);

    return { data, total, page: parseInt(page, 10), pageSize: take, totalPages: Math.ceil(total / take) };
  });

  app.patch('/:id/status', async (request, reply) => {
    const { id } = request.params as { id: string };
    const { status } = request.body as { status: string };
    const validStatuses = ['PENDING', 'CONFIRMED', 'DELIVERED', 'CANCELLED'];
    if (!validStatuses.includes(status)) {
      return reply.status(400).send({ message: 'Invalid status' });
    }
    const order = await app.prisma.order.update({
      where: { id },
      data: { status: status as 'PENDING' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED' },
    });
    return order;
  });
};
