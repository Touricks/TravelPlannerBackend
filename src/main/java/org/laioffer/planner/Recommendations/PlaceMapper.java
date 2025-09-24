package org.laioffer.planner.Recommendations;

import org.laioffer.planner.entity.PlaceEntity;
import org.laioffer.planner.model.common.GeoPoint;
import org.laioffer.planner.model.place.ContactDTO;
import org.laioffer.planner.model.place.OpeningHoursDTO;
import org.laioffer.planner.model.place.PlaceDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PlaceMapper {
    
    /**
     * Convert PlaceEntity to PlaceDTO
     */
    public PlaceDTO toDTO(PlaceEntity entity) {
        if (entity == null) {
            return null;
        }
        
        PlaceDTO dto = new PlaceDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setImageUrl(entity.getImageUrl());
        dto.setDescription(entity.getDescription());
        
        // Convert coordinates to GeoPoint
        if (entity.getLatitude() != null && entity.getLongitude() != null) {
            GeoPoint location = new GeoPoint(
                entity.getLatitude().doubleValue(),
                entity.getLongitude().doubleValue()
            );
            dto.setLocation(location);
        }
        
        // Convert contact info from JSONB
        ContactDTO contact = extractContactInfo(entity);
        if (contact != null) {
            dto.setContact(contact);
        }
        
        // Convert opening hours from JSONB
        OpeningHoursDTO openingHours = extractOpeningHours(entity);
        if (openingHours != null) {
            dto.setOpeningHours(openingHours);
        }
        
        return dto;
    }
    
    /**
     * Extract contact information from entity
     */
    private ContactDTO extractContactInfo(PlaceEntity entity) {
        ContactDTO contact = new ContactDTO();
        boolean hasContact = false;
        
        // Use direct fields first
        if (entity.getWebsite() != null) {
            contact.setWebsite(entity.getWebsite());
            hasContact = true;
        }
        
        if (entity.getPhone() != null) {
            contact.setPhone(entity.getPhone());
            hasContact = true;
        }
        
        // Try to extract from contactInfo JSONB if direct fields are null
        if (!hasContact && entity.getContactInfo() != null) {
            Map<String, Object> contactInfo = entity.getContactInfo();
            
            Object website = contactInfo.get("website");
            if (website instanceof String) {
                contact.setWebsite((String) website);
                hasContact = true;
            }
            
            Object phone = contactInfo.get("phone");
            if (phone instanceof String) {
                contact.setPhone((String) phone);
                hasContact = true;
            }
        }
        
        return hasContact ? contact : null;
    }
    
    /**
     * Extract opening hours from entity
     */
    private OpeningHoursDTO extractOpeningHours(PlaceEntity entity) {
        if (entity.getOpeningHours() == null) {
            return null;
        }
        
        Map<String, Object> hoursData = entity.getOpeningHours();
        
        // For now, just return the raw data as a string
        // In the future, this could be enhanced to parse structured hours
        OpeningHoursDTO openingHours = new OpeningHoursDTO();
        
        // Try to extract raw text or convert map to string
        Object rawHours = hoursData.get("raw");
        if (rawHours instanceof String) {
            openingHours.setRaw((String) rawHours);
        } else {
            // Convert the entire map to a readable string as fallback
            openingHours.setRaw(hoursData.toString());
        }
        
        // TODO: Parse normalized hours structure when needed
        // This would require defining DailyHours and TimeRange classes
        
        return openingHours;
    }
    
    /**
     * Utility method to safely convert BigDecimal to double
     */
    private Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}