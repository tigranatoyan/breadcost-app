-- BC-3004: Customer notification preferences
ALTER TABLE customers ADD COLUMN whatsapp_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE customers ADD COLUMN email_enabled    BOOLEAN DEFAULT TRUE;
ALTER TABLE customers ADD COLUMN push_enabled     BOOLEAN DEFAULT TRUE;
