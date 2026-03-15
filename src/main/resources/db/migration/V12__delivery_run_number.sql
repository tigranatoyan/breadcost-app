-- BC-262: Add sequential run_number to delivery_runs
ALTER TABLE delivery_runs ADD COLUMN run_number INTEGER;

-- Back-fill existing rows with a sequential number per tenant
UPDATE delivery_runs dr
SET run_number = sub.rn
FROM (
  SELECT run_id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY created_at) AS rn
  FROM delivery_runs
) sub
WHERE dr.run_id = sub.run_id;
