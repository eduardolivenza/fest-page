import type { FastifyPluginAsync } from 'fastify';

export const adminContactsRoutes: FastifyPluginAsync = async (app) => {
  app.addHook('preHandler', app.authenticate);

  app.get('/', async (request) => {
    const { read, page = '1', pageSize = '20' } = request.query as Record<string, string>;
    const take = Math.min(parseInt(pageSize, 10), 100);
    const skip = (parseInt(page, 10) - 1) * take;

    const where = read !== undefined ? { read: read === 'true' } : {};

    const [data, total] = await Promise.all([
      app.prisma.contactRequest.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        take,
        skip,
      }),
      app.prisma.contactRequest.count({ where }),
    ]);

    return { data, total, page: parseInt(page, 10), pageSize: take, totalPages: Math.ceil(total / take) };
  });

  app.patch('/:id/read', async (request) => {
    const { id } = request.params as { id: string };
    return app.prisma.contactRequest.update({ where: { id }, data: { read: true } });
  });
};
