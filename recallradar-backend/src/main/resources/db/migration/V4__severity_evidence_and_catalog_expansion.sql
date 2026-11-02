-- Migration V4: Severity evidence tracking, classification sources, and catalog expansion

-- 1. Add evidence and classification_source columns to recall
ALTER TABLE recall ADD COLUMN IF NOT EXISTS severity_evidence TEXT;
ALTER TABLE recall ADD COLUMN IF NOT EXISTS severity_classification_source VARCHAR(150);

-- 2. Add evidence and classification_source columns to recall_severity_audit
ALTER TABLE recall_severity_audit ADD COLUMN IF NOT EXISTS evidence TEXT;
ALTER TABLE recall_severity_audit ADD COLUMN IF NOT EXISTS classification_source VARCHAR(150);

-- 3. Register catalogue products matching regulatory recall notices
INSERT INTO product (name, brand, category, model_number, batch_number)
VALUES
('Scotch Thermal Laminator', '3M Company', 'Appliances', 'TL901', '3M-LAM-2024'),
('Van Rysel GRVL AF Gravel Bicycle', 'Decathlon', 'Sports & Outdoor', 'GRVL-AF', 'DEC-VR-2025'),
('Premier Trimmer Mower', 'DR Power Equipment', 'Tools & Garden', 'TR4-PREMIER', 'DR-TR-2024'),
('SureLock LED Key Bike Lock Set', 'Giant Bicycle', 'Sports & Outdoor', 'SL-KEY-24', 'GB-SL-2024'),
('Convertible Bedside Sleeper Crib', 'Papablic', 'Baby & Nursery', 'BS-01-SLP', 'PAP-BS-2024'),
('CIYTI Light-Up Princess Costume', 'CIYTI', 'Toys & Games', 'COST-PRIN-01', 'CIY-2024'),
('Convertible 6-in-1 Baby High Chair', 'Costway', 'Baby & Nursery', 'HC-6IN1', 'COST-HC-2024'),
('Blitz Full-Auto Air Rifle', 'HatsanUSA', 'Sporting Goods', 'BLITZ-25', 'HAT-BLZ-2024'),
('RSX Luminaire with Tilt Arm Mounting', 'Lithonia Lighting', 'Electronics', 'RSX-ARM', 'LITH-RSX-24'),
('Propane Gas Patio Heater', 'VQQQ', 'Home & Outdoor', 'PH-PROP-01', 'VQQQ-PH-24'),
('LED Projecting Finger Light Toys', 'BAMAMON', 'Toys & Games', 'FL-LED-04', 'BAM-FL-2024'),
('Xalirium Portable Adult Bed Rail', 'usOpal', 'Medical & Safety', 'XAL-BR-01', 'USO-XAL-24'),
('Infant Safety Bath Seat', 'ONTHEWEI', 'Baby & Nursery', 'BS-INFANT-02', 'ONW-BS-2024'),
('Crystal Infused Glass Cups', 'Loving Thyself Rocks', 'Kitchenware', 'CGC-ROCKS-01', 'LTR-CUP-2024')
ON CONFLICT DO NOTHING;
