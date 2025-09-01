/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.ents;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 *
 * @author jjackson
 */

// ProjectState is generated code by Umple
// have to add annotations for now, but want to put in hbm.xml:
//   @Enumerated
//  @JdbcType(PostgreSQLEnumJdbcType.class)
//or
//  @Enumerated(EnumType.STRING)
//
// Sm also @Basic(optional = false), SmS2 has NULLs


@Entity
@Table(name = "project_state")
public class ProjectStateImpl extends ProjectState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull  // not affecting schema generation, use nullable = false, maybe it is validator-only?
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proj_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project"))
    // an seemingly extra unique constraint (index) appears in schema
    // only if Hibernate is creating the table - not added to existing table
    private Project project;
    
    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getProjState_id() {
        return this.id;
    }

    public void setProjState_id(int id) {
        this.id = id;
    }

    public Project getProj() {
        return this.project;
    }

    public void setProj(Project project) {
        this.project = project;
    }
    
}
