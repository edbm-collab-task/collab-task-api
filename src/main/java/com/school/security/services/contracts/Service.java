package com.school.security.services.contracts;

import java.util.List;

public interface Service<Q, R, ID> {
    R createOrUpdate(Q toSave);

    List<R> findAll();

    R findById(ID id);

    R deleteById(ID id);
}
