import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';
import { verifyTurnstile } from '../lib/turnstile.js';

const createContactSchema = z.object({
  name: z.string().min(2).max(100),
  email: z.string().email(),
  phone: z.string().optional(),
  message: z.string().min(10).max(2000),
  turnstileToken: z.string().max(2048).optional(),
});

export const contactsRoutes: FastifyPluginAsync = async (app) => {
  app.post('/', async (request, reply) => {
    const body = createContactSchema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({ message: 'Invalid data', errors: body.error.flatten() });
    }

    const { turnstileToken, ...data } = body.data;
    // Behind the Cloudflare Tunnel, request.ip is cloudflared; the visitor IP is in this header.
    const visitorIp = request.headers['cf-connecting-ip'];
    const human = await verifyTurnstile(turnstileToken, typeof visitorIp === 'string' ? visitorIp : undefined);
    if (!human) {
      return reply.status(403).send({ message: 'Captcha verification failed' });
    }

    const contact = await app.prisma.contactRequest.create({ data });
    return reply.status(201).send(contact);
  });
};
