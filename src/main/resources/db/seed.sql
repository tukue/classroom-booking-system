-- Demonstration inventory until the facility supplies its verified room data.
INSERT INTO equipment (equipment_id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'PROJECTOR'),
    ('00000000-0000-0000-0000-000000000002', 'WHITEBOARD')
ON CONFLICT (name) DO NOTHING;

INSERT INTO classrooms (classroom_id, name, seating_capacity, accessible) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Training Room 01', 8, TRUE),
    ('10000000-0000-0000-0000-000000000002', 'Training Room 02', 10, TRUE),
    ('10000000-0000-0000-0000-000000000003', 'Training Room 03', 12, FALSE),
    ('10000000-0000-0000-0000-000000000004', 'Training Room 04', 16, TRUE),
    ('10000000-0000-0000-0000-000000000005', 'Training Room 05', 20, TRUE),
    ('10000000-0000-0000-0000-000000000006', 'Training Room 06', 24, FALSE),
    ('10000000-0000-0000-0000-000000000007', 'Training Room 07', 28, TRUE),
    ('10000000-0000-0000-0000-000000000008', 'Training Room 08', 30, TRUE),
    ('10000000-0000-0000-0000-000000000009', 'Training Room 09', 32, FALSE),
    ('10000000-0000-0000-0000-000000000010', 'Training Room 10', 36, TRUE),
    ('10000000-0000-0000-0000-000000000011', 'Training Room 11', 40, TRUE),
    ('10000000-0000-0000-0000-000000000012', 'Training Room 12', 45, FALSE),
    ('10000000-0000-0000-0000-000000000013', 'Training Room 13', 50, TRUE),
    ('10000000-0000-0000-0000-000000000014', 'Training Room 14', 55, TRUE),
    ('10000000-0000-0000-0000-000000000015', 'Training Room 15', 60, FALSE),
    ('10000000-0000-0000-0000-000000000016', 'Training Room 16', 65, TRUE),
    ('10000000-0000-0000-0000-000000000017', 'Training Room 17', 70, TRUE),
    ('10000000-0000-0000-0000-000000000018', 'Training Room 18', 80, FALSE),
    ('10000000-0000-0000-0000-000000000019', 'Training Room 19', 100, TRUE),
    ('10000000-0000-0000-0000-000000000020', 'Training Room 20', 120, TRUE)
ON CONFLICT (classroom_id) DO NOTHING;

INSERT INTO classroom_equipment (classroom_id, equipment_id)
SELECT classrooms.classroom_id, equipment.equipment_id
FROM (VALUES
    ('Training Room 01', 'WHITEBOARD'), ('Training Room 01', 'PROJECTOR'),
    ('Training Room 02', 'WHITEBOARD'),
    ('Training Room 03', 'WHITEBOARD'), ('Training Room 03', 'PROJECTOR'),
    ('Training Room 04', 'WHITEBOARD'),
    ('Training Room 05', 'WHITEBOARD'), ('Training Room 05', 'PROJECTOR'),
    ('Training Room 06', 'WHITEBOARD'),
    ('Training Room 07', 'WHITEBOARD'), ('Training Room 07', 'PROJECTOR'),
    ('Training Room 08', 'WHITEBOARD'),
    ('Training Room 09', 'WHITEBOARD'), ('Training Room 09', 'PROJECTOR'),
    ('Training Room 10', 'WHITEBOARD'),
    ('Training Room 11', 'WHITEBOARD'), ('Training Room 11', 'PROJECTOR'),
    ('Training Room 12', 'WHITEBOARD'),
    ('Training Room 13', 'WHITEBOARD'), ('Training Room 13', 'PROJECTOR'),
    ('Training Room 14', 'WHITEBOARD'),
    ('Training Room 15', 'WHITEBOARD'), ('Training Room 15', 'PROJECTOR'),
    ('Training Room 16', 'WHITEBOARD'),
    ('Training Room 17', 'WHITEBOARD'), ('Training Room 17', 'PROJECTOR'),
    ('Training Room 18', 'WHITEBOARD'),
    ('Training Room 19', 'WHITEBOARD'), ('Training Room 19', 'PROJECTOR'),
    ('Training Room 20', 'WHITEBOARD'), ('Training Room 20', 'PROJECTOR')
) AS seed(room_name, equipment_name)
JOIN classrooms ON classrooms.name = seed.room_name
JOIN equipment ON equipment.name = seed.equipment_name
ON CONFLICT (classroom_id, equipment_id) DO NOTHING;
