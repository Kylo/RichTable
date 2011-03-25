package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 *
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class PageContainerProvider implements Property.ValueChangeListener {

    private final Container.Indexed dataContainer;
    private final int itemsPerPage;
    private final int numberOfPages;
    private int currentPageNumber = 0;
    private Container currentContainer;

    public PageContainerProvider(Container.Indexed dataContainer, int itemsPerPage) {
        this.dataContainer = dataContainer;
        this.itemsPerPage = itemsPerPage;
        this.numberOfPages = calculateNumberOfPages(itemsPerPage, dataContainer.size());
    }

    public PageContainerProvider(Container.Indexed dataContainer) {
        this(dataContainer, 100);
    }

    public Container getPageContainer(int pageNumber) {
        currentPageNumber = pageNumber - 1;
        if (!isPageAvailable(currentPageNumber)) {
            return null;
        }
        currentContainer = fillCurrentContainer();

        return currentContainer;
    }

    public int getLastPageNumber() {
        return numberOfPages;
    }

    private int calculateNumberOfPages(int itemsPerPage, int containerSize) {
        if (itemsPerPage <= 0) {
            return 1;
        } else {
            return (containerSize / itemsPerPage) + (containerSize % itemsPerPage > 0 ? 1 : 0);
        }
    }

    private Container fillCurrentContainer() {
        // If you change IndexContainer make sure that valueChange(Property.ValueChangeEvent) method works well
        IndexedContainer container = new IndexedContainer();
        for (Object object : dataContainer.getContainerPropertyIds()) {
            container.addContainerProperty(object, dataContainer.getType(object), null);
        }
        copyItemsToContainer(container, firstRowIdx(currentPageNumber), lastRowIdx(currentPageNumber));
        container.addListener((Property.ValueChangeListener) this);
        return container;
    }

    private int firstRowIdx(int page) {
        return page * itemsPerPage;
    }

    private int lastRowIdx(int page) {
        return Math.min((page + 1) * itemsPerPage, dataContainer.size());
    }

    private void copyItemsToContainer(Container container, int start, int stop) {
        for (int i = start; i < stop; ++i) {
            Object id = dataContainer.getIdByIndex(i);
            copyItemToContainer(id, container);
        }
    }

    private void copyItemToContainer(Object itemId, Container container) {
        Item realItem = dataContainer.getItem(itemId);
        Item shownItem = container.addItem(itemId);
        for (Object property : dataContainer.getContainerPropertyIds()) {
            shownItem.getItemProperty(property).setValue(
                    realItem.getItemProperty(property).getValue());
        }
    }

    public void hideRows(Collection<?> rows) {
        for (Object item : rows) {
            currentContainer.removeItem(item);
        }
    }

    public void showHiddenRows() {
        currentContainer.removeAllItems();
        copyItemsToContainer(currentContainer, firstRowIdx(currentPageNumber), lastRowIdx(currentPageNumber));
    }

    private boolean isPageAvailable(int pageNumber) {
        return pageNumber < numberOfPages;
    }

    void sort(Object sortContainerPropertyId, boolean sortAscending) {
        final Container c = dataContainer;
        if (c instanceof Container.Sortable) {
            ((Container.Sortable) c).sort(
                    new Object[]{sortContainerPropertyId},
                    new boolean[]{sortAscending});
        } else if (c != null) {
            throw new UnsupportedOperationException(
                    "Underlying Data does not allow sorting");
        }
    }

    public void valueChange(Property.ValueChangeEvent event) {
        // Reflection hack neccessary to access information which item was changed
        // It is pretty secure because currentContainer is instance of IndexedContainer
        // which properties provide both fields - itemId and propertyId
        Property property = event.getProperty();
        try {
            Field itemIdField = property.getClass().getDeclaredField("itemId");
            itemIdField.setAccessible(true);
            Object itemId = itemIdField.get(property);
            Field propertyIdField = property.getClass().getDeclaredField("propertyId");
            propertyIdField.setAccessible(true);
            Object propertyId = propertyIdField.get(property);
            dataContainer.getItem(itemId).getItemProperty(propertyId).setValue(property.getValue());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedOperationException("Error while trying to store data", ex);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException("Error while trying to store data", ex);
        } catch (NoSuchFieldException ex) {
            throw new UnsupportedOperationException("Error while trying to store data", ex);
        } catch (SecurityException ex) {
            throw new UnsupportedOperationException("Error while trying to store data", ex);
        }
    }
}
