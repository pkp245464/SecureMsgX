package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasskeyRepository extends JpaRepository<Passkey, String> {

}

