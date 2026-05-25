import type { FastifyPluginAsync } from 'fastify';
import { verify } from 'argon2';
import { z } from 'zod';

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export const adminAuthRoutes: FastifyPluginAsync = async (app) => {
  app.post('/login', async (request, reply) => {
    const body = loginSchema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({ message: 'Invalid credentials' });
    }

    const admin = await app.prisma.adminUser.findUnique({
      where: { email: body.data.email },
    });

    if (!admin) {
      return reply.status(401).send({ message: 'Invalid credentials' });
    }

    const valid = await verify(admin.passwordHash, body.data.password);
    if (!valid) {
      return reply.status(401).send({ message: 'Invalid credentials' });
    }

    const token = app.jwt.sign({ sub: admin.id, email: admin.email });

    return {
      token,
      user: { id: admin.id, email: admin.email, name: admin.name },
    };
  });

  app.get('/me', { preHandler: [app.authenticate] }, async (request) => {
    const admin = await app.prisma.adminUser.findUniqueOrThrow({
      where: { id: request.user.id },
      select: { id: true, email: true, name: true, createdAt: true },
    });
    return admin;
  });
};
