ALTER TABLE experiments
  ADD COLUMN participant_limit INT UNSIGNED DEFAULT NULL COMMENT '实验人数上限' AFTER location;
