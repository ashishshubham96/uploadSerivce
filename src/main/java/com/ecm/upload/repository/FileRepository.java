package com.ecm.upload.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecm.upload.entity.File;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    // You can add custom query methods here if needed
}