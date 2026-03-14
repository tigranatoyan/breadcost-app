-- BC-286: Change default currency from UZS to AMD
ALTER TABLE tenant_config ALTER COLUMN main_currency SET DEFAULT 'AMD';
UPDATE tenant_config SET main_currency = 'AMD' WHERE main_currency = 'UZS';
