import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';

const createContactSchema = z.object({
  name: z.string().min(2).max(100),
  email: z.string().email(),
  phone: z.string().optional(),
  message: z.string().min(10).max(2000),
});

export const contactsRoutes: FastifyPluginAsync = async (app) => {
  app.post('/', async (request, reply) => {
    const body = createContactSchema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({ message: 'Invalid data', errors: body.error.flatten() });
    }

    const contact = await app.prisma.contactRequest.create({ data: body.data });
    return reply.status(201).send(contact);
  });
};
