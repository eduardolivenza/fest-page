import { PrismaClient } from '@prisma/client';
import { hash } from 'argon2';

const prisma = new PrismaClient();

async function main() {
  // Seed admin user
  const adminPassword = await hash('admin123!');
  await prisma.adminUser.upsert({
    where: { email: 'admin@festpage.com' },
    update: {},
    create: {
      email: 'admin@festpage.com',
      passwordHash: adminPassword,
      name: 'Admin',
    },
  });

  // Seed sample products
  const products = [
    {
      name: 'Canapé Nordic Dream',
      slug: 'canape-nordic-dream',
      description: 'Cama de madera maciza estilo nórdico con cabecero tapizado.',
      price: 899.99,
      category: 'BED' as const,

      featured: true,
    },
    {
      name: 'Colchón Comfort Plus',
      slug: 'colchon-comfort-plus',
      description: 'Colchón viscoelástico de alta densidad con tecnología de muelles ensacados.',
      price: 649.99,
      category: 'MATTRESS' as const,

      featured: true,
    },
    {
      name: 'Almohada Cervical Premium',
      slug: 'almohada-cervical-premium',
      description: 'Almohada ergonómica de látex natural con funda de bambú.',
      price: 79.99,
      category: 'PILLOW' as const,

      featured: false,
    },
    {
      name: 'Somier Articulado Deluxe',
      slug: 'somier-articulado-deluxe',
      description: 'Somier articulado eléctrico con mando a distancia y memoria de posiciones.',
      price: 1199.99,
      category: 'BED' as const,

      featured: true,
    },
    {
      name: 'Colchón Látex Natural',
      slug: 'colchon-latex-natural',
      description: 'Colchón 100% látex natural con certificado Oeko-Tex. Transpirable y antialérgico.',
      price: 849.99,
      category: 'MATTRESS' as const,

      featured: false,
    },
    {
      name: 'Pack Almohadas DreamSoft',
      slug: 'pack-almohadas-dreamsoft',
      description: 'Pack de 2 almohadas de fibra hueca con efecto cuscús. Suaves y lavables.',
      price: 49.99,
      category: 'PILLOW' as const,

      featured: false,
    },
  ];

  for (const product of products) {
    await prisma.product.upsert({
      where: { slug: product.slug },
      update: {},
      create: product,
    });
  }

  console.log('Seed complete.');
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
