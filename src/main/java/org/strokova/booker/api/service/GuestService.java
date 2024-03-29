package org.strokova.booker.api.service;

import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.strokova.booker.api.entity.GuestEntity;
import org.strokova.booker.api.entity.GuestEntityFactory;
import org.strokova.booker.api.entityParameters.GuestParameters;
import org.strokova.booker.api.repository.GuestRepository;
import org.strokova.booker.common.model.Guest;

import static org.strokova.booker.api.searchPredicate.GuestSearchPredicates.nameIs;
import static org.strokova.booker.api.searchPredicate.GuestSearchPredicates.phoneIs;
import static org.strokova.booker.api.service.ServiceUtils.determineSortDirection;

/**
 * 02.11.2016.
 */
@Service
public class GuestService {

    private final GuestRepository guestRepository;

    @Autowired
    public GuestService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Transactional
    public Guest saveGuest(Guest guest) {
        return new Guest(guestRepository.save(GuestEntityFactory.create(guest)));
    }

    @Transactional(readOnly = true)
    public Page<Guest> readGuests(Integer page, Integer size, String order, String by, String name, String phone) {
        return guestRepository.findAll(
                createSearchPredicate(name, phone),
                createPageRequest(page, size, order, by))
                .map(Guest::new);
    }

    @Transactional(readOnly = true)
    public Guest readGuest(Long guestId) {
        return new Guest(guestRepository.findOne(guestId));
    }

    @Transactional
    public void deleteGuest(Long guestId) {
        guestRepository.delete(guestId);
    }

    @Transactional
    public Guest updateGuest(Long guestId, Guest newGuestData) {
        Long newId = newGuestData.getId();
        if (newId != null && !newId.equals(guestId)) {
            throw new IllegalArgumentException("Impossible to update guest id");
        }
        GuestEntity oldGuestEntity = guestRepository.findOne(guestId);
        return new Guest(guestRepository.save(updateGuestData(oldGuestEntity, newGuestData)));
    }

    private static GuestEntity updateGuestData(GuestEntity guest, Guest data) {
        return guest
                .setName(data.getName())
                .setPhone((data.getPhone()));
    }

    private static BooleanBuilder createSearchPredicate(String name, String phone) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (name != null && !name.trim().isEmpty()) {
            predicate.and(nameIs(name));
        }
        if (phone != null && !phone.trim().isEmpty()) {
            predicate.and(phoneIs(phone));
        }
        return predicate;
    }

    private static PageRequest createPageRequest(Integer page, Integer size, String order, String by) {
        return new PageRequest(page, size, determineSortDirection(order), determineSortProperty(by));
    }

    // TODO: can generify these methods?
    private static String determineSortProperty(String by) {
        String sortProperty = null;

        for (GuestParameters param : GuestParameters.values()) {
            if (by.equalsIgnoreCase(param.getQueryParameterName())) {
                sortProperty = param.getColumnName();
            }
        }
        // if by param is invalid - order by name
        if (sortProperty == null) {
            sortProperty = GuestParameters.NAME.getColumnName();
        }

        return sortProperty;
    }
}
