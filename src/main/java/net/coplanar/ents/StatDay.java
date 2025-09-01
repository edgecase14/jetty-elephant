package net.coplanar.ents;

import java.io.Serializable;
import java.lang.String;
import jakarta.persistence.*;
import java.time.LocalDate;


/**
 * Entity implementation class for Entity: Rep
 *
 */
@Entity

public class StatDay implements Serializable {
   
	@Id
	@GeneratedValue ( strategy = GenerationType.IDENTITY )
	private int holiday_id;
	@Column(nullable=false)
	private LocalDate holiday;
	@Column(nullable=false)
	private String holiday_name;
	private static final long serialVersionUID = 1L;

	public StatDay() {
		super();
	}   
	public int getHolidayId() {
		return this.holiday_id;
	}

	public void setHolidayId(int id) {
		this.holiday_id = id;
	}   
	public LocalDate getHolidayDate() {
		return this.holiday;
	}

	public void setHolidayDate(LocalDate holiday) {
		this.holiday = holiday;
	}
	public String getHolidayName() {
		return this.holiday_name;
	}

	public void setHolidayName(String holiday_name) {
		this.holiday_name = holiday_name;
	}
   
}
