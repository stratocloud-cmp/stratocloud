package com.stratocloud.jpa.repository;

import com.stratocloud.jpa.entities.Auditable;

public interface AuditableRepository<E extends Auditable> extends Repository<E, Long>{
}
