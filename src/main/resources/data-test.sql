-- Script SQL pour créer des utilisateurs de test dans la base de données HR

-- Créer une équipe
INSERT INTO teams (id, nom, description) VALUES
(1, 'Équipe Développement', 'Équipe de développement logiciel');

-- Créer un RH (ID=1)
INSERT INTO users (id, matricule, nom, prenom, email, password, poste, departement, date_embauche, solde_conge, role, team_id, manager_id)
VALUES
(1, 'RH001', 'Admin', 'RH', 'rh@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Responsable RH', 'Ressources Humaines', '2020-01-01', NULL, 'HR', NULL, NULL);

-- Créer un Manager (ID=2)
INSERT INTO users (id, matricule, nom, prenom, email, password, poste, departement, date_embauche, solde_conge, role, team_id, manager_id)
VALUES
(2, 'MGR001', 'Dupont', 'Jean', 'jean.dupont@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manager', 'IT', '2020-06-01', 25, 'MANAGER', 1, NULL);

-- Créer des employés (ID=3, 4, 5, 6)
INSERT INTO users (id, matricule, nom, prenom, email, password, poste, departement, date_embauche, solde_conge, role, team_id, manager_id)
VALUES
(3, 'EMP001', 'Martin', 'Sophie', 'sophie.martin@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Développeur', 'IT', '2021-03-15', 25, 'EMPLOYEE', 1, 2),
(4, 'EMP002', 'Bernard', 'Luc', 'luc.bernard@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Développeur', 'IT', '2021-06-01', 22, 'EMPLOYEE', 1, 2),
(5, 'EMP003', 'Petit', 'Marie', 'marie.petit@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Développeur', 'IT', '2022-01-10', 25, 'EMPLOYEE', 1, 2),
(6, 'EMP004', 'Durand', 'Pierre', 'pierre.durand@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Développeur', 'IT', '2022-03-01', 20, 'EMPLOYEE', 1, 2);

-- Réinitialiser les séquences (pour PostgreSQL)
-- ALTER SEQUENCE users_id_seq RESTART WITH 7;
-- ALTER SEQUENCE teams_id_seq RESTART WITH 2;

-- Pour MySQL, utilisez :
-- ALTER TABLE users AUTO_INCREMENT = 7;
-- ALTER TABLE teams AUTO_INCREMENT = 2;

-- Vérification
SELECT id, matricule, nom, prenom, email, role FROM users;

-- Note : Le mot de passe par défaut pour tous les utilisateurs est : "password123"
-- (hashé avec BCrypt : $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy)
