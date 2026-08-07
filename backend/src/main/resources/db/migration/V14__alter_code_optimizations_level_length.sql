-- V14 Extend optimization_level column length to 255
-- CodePilot AI Code Optimizer Module

ALTER TABLE code_optimizations ALTER COLUMN optimization_level VARCHAR(255) NOT NULL;
