/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.updatemsg;

/**
 *
 * @author jjackson
 */
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.SIMPLE_NAME, include=As.PROPERTY, property="type") // Include Java class simple-name as JSON property "type"
// could omit outbound-only messages?
@JsonSubTypes({@Type(CellUpdate.class), @Type(CellList.class), @Type(GotRow.class), @Type(ShowUsername.class), @Type(PointAdd.class)}) // Required for deserialization only  
public abstract class UpdateMessage {
}
