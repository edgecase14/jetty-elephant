/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.34.0.7242.6b8819789 modeling language!*/

package net.coplanar.ents;
import jakarta.persistence.Basic;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@MappedSuperclass
// line 15 "../../../ProjectState.ump"
public abstract class ProjectState
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //ProjectState State Machines
  public enum Sm { s1, s2 }
  public enum SmS2 { Null, s2a, s2b }
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Basic(optional = false)
  private Sm sm;
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private SmS2 smS2;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public ProjectState()
  {
    setSmS2(SmS2.Null);
    setSm(Sm.s1);
  }

  //------------------------
  // INTERFACE
  //------------------------

  public String getSmFullName()
  {
    String answer = sm.toString();
    if (smS2 != SmS2.Null) { answer += "." + smS2.toString(); }
    return answer;
  }

  public Sm getSm()
  {
    return sm;
  }

  public SmS2 getSmS2()
  {
    return smS2;
  }

  public boolean e1()
  {
    boolean wasEventProcessed = false;
    
    Sm aSm = sm;
    switch (aSm)
    {
      case s1:
        setSm(Sm.s2);
        wasEventProcessed = true;
        break;
      case s2:
        exitSm();
        setSm(Sm.s1);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean e2()
  {
    boolean wasEventProcessed = false;
    
    Sm aSm = sm;
    switch (aSm)
    {
      case s1:
        setSmS2(SmS2.s2b);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean e3()
  {
    boolean wasEventProcessed = false;
    
    SmS2 aSmS2 = smS2;
    switch (aSmS2)
    {
      case s2a:
        exitSmS2();
        setSmS2(SmS2.s2b);
        wasEventProcessed = true;
        break;
      case s2b:
        exitSmS2();
        setSmS2(SmS2.s2a);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private void exitSm()
  {
    switch(sm)
    {
      case s2:
        exitSmS2();
        break;
    }
  }

  private void setSm(Sm aSm)
  {
    sm = aSm;

    // entry actions and do activities
    switch(sm)
    {
      case s2:
        if (smS2 == SmS2.Null) { setSmS2(SmS2.s2a); }
        break;
    }
  }

  private void exitSmS2()
  {
    switch(smS2)
    {
      case s2a:
        setSmS2(SmS2.Null);
        break;
      case s2b:
        setSmS2(SmS2.Null);
        break;
    }
  }

  private void setSmS2(SmS2 aSmS2)
  {
    smS2 = aSmS2;
    if (sm != Sm.s2 && aSmS2 != SmS2.Null) { setSm(Sm.s2); }
  }

  public void delete()
  {}

}