INSERT INTO public.users (id_client,username,username_type,pin_hash,pin_attempts,pin_locked_until,status,last_login_at,device_trusted,biometric_enabled,biometric_enrolled_at,biometric_type,created_at,updated_at,mfa_enabled,mfa_type,mfa_enrolled_at,id) VALUES
    (1,'read424@gmail.com','PHONE','93a91475cca49cf5921eb77271e5cd7f94b59145ec181be5c098281fefeb0f95',0,NULL,1,NULL,false,false,NULL,NULL,'2026-02-16 11:54:59.992857','2026-02-16 11:55:06.51227',false,NULL,NULL,9);

-- Ajustar secuencia al máximo ID insertado
SELECT setval('public.users_id_seq', (SELECT MAX(id_client) FROM public.users));