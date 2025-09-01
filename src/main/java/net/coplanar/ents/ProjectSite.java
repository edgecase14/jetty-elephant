/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.ents;

/**
 *
 * @author jjackson
 */
import java.io.Serializable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.geolatte.geom.Point;

/**
 * Entity implementation class for Entity: Projects
 *
 */
@Entity
@Table(name = "project_site")
public class ProjectSite implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @NotNull // ditto
    @Column(name = "geom", nullable = false, columnDefinition = "geometry(Point,22717)")
    private Point point;

    public int getProjSite_id() {
        return this.id;
    }

    public void setProjSite_id(int id) {
        this.id = id;
    }

    public Project getProj() {
        return this.project;
    }

    public void setProj(Project project) {
        this.project = project;
    }

    public Point getPoint() {
        return this.point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }
}