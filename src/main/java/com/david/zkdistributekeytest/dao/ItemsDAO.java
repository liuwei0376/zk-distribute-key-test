package com.david.zkdistributekeytest.dao;

import com.david.zkdistributekeytest.domain.Items;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemsDAO extends JpaRepository<Items, String> {
}
