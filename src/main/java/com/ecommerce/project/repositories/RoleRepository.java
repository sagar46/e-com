package com.ecommerce.project.repositories;

import com.ecommerce.project.entities.AppRole;
import com.ecommerce.project.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole appRole);
}
