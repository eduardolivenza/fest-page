-- Local development data (replaces packages/db/src/seed.ts). Never run it against production.
-- Idempotent: rows that already exist are left alone.
--   docker compose exec -T postgres psql -U festpage -d festpage < apps/api/dev/seed.sql
-- Admin login: admin@festpage.com / admin123!

INSERT INTO "AdminUser" (id, email, "passwordHash", name, "updatedAt")
VALUES ('cdevadmin00000000000000001', 'admin@festpage.com',
        '$argon2id$v=19$m=65536,t=3,p=4$eMZcvkriW+egj473uE63gw$wm5/Tt60LBX8ckK8UAhuV2e8DEpr2VvMOeBi0YPD6Ro',
        'Admin', now() AT TIME ZONE 'UTC')
ON CONFLICT (email) DO NOTHING;

INSERT INTO "Product" (id, name, slug, description, price, category, featured, "createdAt", "updatedAt")
VALUES
  ('cdevproduct000000000000001', 'Canapé Nordic Dream', 'canape-nordic-dream',
   'Cama de madera maciza estilo nórdico con cabecero tapizado.', 899.99, 'BED', true,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'),
  ('cdevproduct000000000000002', 'Colchón Comfort Plus', 'colchon-comfort-plus',
   'Colchón viscoelástico de alta densidad con tecnología de muelles ensacados.', 649.99, 'MATTRESS', true,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'),
  ('cdevproduct000000000000003', 'Almohada Cervical Premium', 'almohada-cervical-premium',
   'Almohada ergonómica de látex natural con funda de bambú.', 79.99, 'PILLOW', false,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'),
  ('cdevproduct000000000000004', 'Somier Articulado Deluxe', 'somier-articulado-deluxe',
   'Somier articulado eléctrico con mando a distancia y memoria de posiciones.', 1199.99, 'BED', true,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'),
  ('cdevproduct000000000000005', 'Colchón Látex Natural', 'colchon-latex-natural',
   'Colchón 100% látex natural con certificado Oeko-Tex. Transpirable y antialérgico.', 849.99, 'MATTRESS', false,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'),
  ('cdevproduct000000000000006', 'Pack Almohadas DreamSoft', 'pack-almohadas-dreamsoft',
   'Pack de 2 almohadas de fibra hueca con efecto cuscús. Suaves y lavables.', 49.99, 'PILLOW', false,
   now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC')
ON CONFLICT (slug) DO NOTHING;
