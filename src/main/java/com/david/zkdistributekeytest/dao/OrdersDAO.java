package com.david.zkdistributekeytest.dao;

import com.david.zkdistributekeytest.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersDAO extends JpaRepository<Orders,String> {
}
