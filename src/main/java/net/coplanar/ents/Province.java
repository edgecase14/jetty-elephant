/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.ents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 *
 * @author jjackson
 */
@Entity
public class Province {
    @Id
    @Column(length = 2, columnDefinition = "CHARACTER(2)")
    private String prov;
    
    @Column(nullable = false, unique = true, length = 25)
    private String prov_name;
}
