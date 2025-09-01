package net.coplanar.ents;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Types;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Entity implementation class for Entity: TsCell
 *
 */
@Entity
@Table ( name = "master2",
		indexes = @Index (name = "idx_te_date2", columnList = "DATE", unique = false)
		) // doesn't exist in prod schema

public class TsCell implements Serializable {

	   
	@Id
	@GeneratedValue ( strategy = GenerationType.IDENTITY )
	private int id;
	
	@ManyToOne(optional = false)
        @NotNull
	@JoinColumn( name="user_id",
			foreignKey = @ForeignKey(name="USER_ID_FK")
	)
	private TsUser tsuser;
	
	@ManyToOne(optional = false)
        @NotNull
	@JoinColumn( name="proj_id",
			foreignKey = @ForeignKey(name="PROJ_ID_FK")
	)
	private Project project;
	
	@Column(name = "date", nullable=false)
	private LocalDate te_date;

        @Column(name = "duration", nullable=false) // prod schema column type "double precision, could use real instead?
	private float entry;

        @Column(name = "note", nullable = false, columnDefinition="TEXT")
        private String te_note;
        
        @Column(name = "period", nullable = false, length = 10) // prod schema column type "text", change to date?
        private String pay_period;
        
        @Column(nullable = false, length = 7)   // was "ID"
        private String old_id;
        
        @Column(nullable = false)
        private String ot;
        
        @Column(length = 36)
        private String txn_id;

	public TsCell() {
		super();
	}   
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}   
	public float getEntry() {
		return this.entry;
	}

	public void setEntry(float entry) {
		this.entry = entry;
	}
	public Project getProject() {
		return this.project;
	}
	public TsUser getTsUser() {
		return this.tsuser;
	}
	public LocalDate getDate() {
		return this.te_date;
	}
	public void setDate(LocalDate dt) {
		this.te_date = dt;
	}
	public String getNote() {
		return this.te_note;
	}
	public void setNote(String note) {
		this.te_note = note;
	}

	// how about a toJson() ?
   
}
