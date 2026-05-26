INSERT INTO public.user_types (id,name,description,is_active,created_at) VALUES
         (1,'client','Cliente del sistema','1','2025-08-27 17:56:21.846-05'),
         (2,'agent','Agente de ventas','1','2025-08-27 17:56:21.846-05'),
         (3,'supervisor','Supervisor de ventas','1','2025-08-27 17:56:21.846-05'),
         (4,'admin','Administrador del sistema','1','2025-08-27 17:56:21.846-05');

-- Ajustar secuencia al máximo ID insertado
SELECT setval('public.user_types_id_seq', (SELECT MAX(id) FROM public.user_types));