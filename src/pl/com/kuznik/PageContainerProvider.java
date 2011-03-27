package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Instances of this class serve pages based on original container
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class PageContainerProvider {

    private final Container.Indexed dataContainer;
    private final int itemsPerPage;
    private int currentPageNumber = 0;
    private ProxyPageContainer currentContainer;

    /**
     * Creates new provider with dataContainer and itemsPerPage
     * @param dataContainer
     * @param itemsPerPage
     */
    public PageContainerProvider(Container.Indexed dataContainer, int itemsPerPage) {
        this.dataContainer = dataContainer;
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Creates new provider with default 100 items per page
     * @param dataContainer
     */
    public PageContainerProvider(Container.Indexed dataContainer) {
        this(dataContainer, 100);
    }

    /**
     * Creates container representing one page
     * @param pageNumber number of page to retrieve
     * @return container representing one page (pageNumber) or null if pageNumber is not valid page
     */
    public Container getPageContainer(int pageNumber) {
        currentPageNumber = pageNumber - 1;
        if (!isPageAvailable(currentPageNumber)) {
            return null;
        }
        if (currentContainer != null) {
            currentContainer.removeParentListener();
        }
        currentContainer = new ProxyPageContainer(dataContainer, currentPageNumber);

        return currentContainer;
    }

    /**
     *
     * @return number of last page (1..MAX_PAGE)
     */
    public int getLastPageNumber() {
        return getNumberOfPages();
    }

    /**
     *
     * @return total number of pages
     */
    private int getNumberOfPages() {
        if (itemsPerPage <= 0 || dataContainer.size() == 0) {
            return 1;
        } else {
            return (dataContainer.size() / itemsPerPage) + (dataContainer.size() % itemsPerPage > 0 ? 1 : 0);
        }
    }

    /**
     * Hides rows supplied in parameter
     * @param rows rows to be hidden
     */
    public void hideRows(Collection<?> rows) {
        currentContainer.hideRows(rows);
    }

    /**
     * shows all hidden rows
     */
    public void showHiddenRows() {
        currentContainer.showHiddenRows();
    }

    private boolean isPageAvailable(int pageNumber) {
        return pageNumber < getNumberOfPages();
    }

    /**
     * Performs sorting on underlying container if it is possible
     * @param sortContainerPropertyId sorting key
     * @param sortAscending true if ascending, false otherwise
     *
     * @throws UnsupportedOperationException if underlying container does not support sorting
     */
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

    /**
     * class responsible for synchronizing data between current page and
     * original container
     */
    private class DataSynchronizer implements Property.ValueChangeListener {

        private Container eventDestination;

        public DataSynchronizer(Container eventDestination) {
            this.eventDestination = eventDestination;
        }

        public void valueChange(ValueChangeEvent event) {
            Property property = event.getProperty();
            try {
                // hack with reflection necessary to access information on
                // propertyId and itemId. It is safe on IndexedContainer
                Field itemIdField = property.getClass().getDeclaredField("itemId");
                itemIdField.setAccessible(true);
                Object itemId = itemIdField.get(property);
                Field propertyIdField = property.getClass().getDeclaredField("propertyId");
                propertyIdField.setAccessible(true);
                Object propertyId = propertyIdField.get(property);
                Item item = eventDestination.getItem(itemId);
                if (item != null) {
                    Object value = item.getItemProperty(propertyId).getValue();
                    if (value == null) {
                        if (value != property.getValue()) {
                            item.getItemProperty(propertyId).setValue(property.getValue());
                        }
                    } else if (!value.equals(property.getValue())) {
                        item.getItemProperty(propertyId).setValue(property.getValue());
                    }
                }
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

    /**
     * instances represent current page
     */
    private class ProxyPageContainer extends IndexedContainer {

        private Container.Indexed parent = null;
        private DataSynchronizer synchronizer;

        public ProxyPageContainer(Container.Indexed parent, int pageNumber) {
            this.parent = parent;
            for (Object object : parent.getContainerPropertyIds()) {
                super.addContainerProperty(object, parent.getType(object), null);
            }
            copyMultipleItemsFromParent(firstRowIdx(pageNumber), lastRowIdx(pageNumber));
            addListener((Property.ValueChangeListener) new DataSynchronizer(parent));
            synchronizer = new DataSynchronizer(this);
            if (parent instanceof Property.ValueChangeNotifier) {
                ((Property.ValueChangeNotifier) parent).addListener((Property.ValueChangeListener) synchronizer);
            }
        }

        private void copySingleItemFromParent(Object itemId) {
            Item item = parent.getItem(itemId);
            Item newItem = super.addItem(itemId);
            for (Object property : item.getItemPropertyIds()) {
                newItem.getItemProperty(property).setValue(
                        item.getItemProperty(property).getValue());
            }
        }

        private void copyMultipleItemsFromParent(int start, int stop) {
            for (int i = start; i < stop; ++i) {
                Object id = parent.getIdByIndex(i);
                copySingleItemFromParent(id);
            }
        }

        private int firstRowIdx(int page) {
            return page * itemsPerPage;
        }

        private int lastRowIdx(int page) {
            return Math.min((page + 1) * itemsPerPage, parent.size());
        }

        public void hideRows(Collection<?> rows) {
            for (Object item : rows) {
                super.removeItem(item); // super to omit overriden method
            }
        }

        public void showHiddenRows() {
            super.removeAllItems(); // super to omit overriden method
            copyMultipleItemsFromParent(firstRowIdx(currentPageNumber), lastRowIdx(currentPageNumber));
        }

        @Override
        public boolean removeItem(Object itemId) throws UnsupportedOperationException {
            return parent.removeItem(itemId);
        }

        @Override
        public boolean removeContainerProperty(Object propertyId) throws UnsupportedOperationException {
            super.removeContainerProperty(propertyId);
            return parent.removeContainerProperty(propertyId);
        }

        @Override
        public boolean removeAllItems() throws UnsupportedOperationException {
            return parent.removeAllItems();
        }

        @Override
        public Object addItem() throws UnsupportedOperationException {
            if (size() < itemsPerPage) {
                super.addItem(); // only when current page requires update
            }
            return parent.addItem();
        }

        @Override
        public Item addItem(Object itemId) throws UnsupportedOperationException {
            if (size() < itemsPerPage) {
                super.addItem(itemId); // only when current page requires update
            }
            return parent.addItem(itemId);
        }

        @Override
        public boolean addContainerProperty(Object propertyId, Class<?> type, Object defaultValue)
                throws UnsupportedOperationException {
            super.addContainerProperty(propertyId, type, defaultValue);
            return parent.addContainerProperty(propertyId, type, defaultValue);
        }

        // called before page change to remove unnecessary listener from parent
        private void removeParentListener() {
            if (parent instanceof Property.ValueChangeNotifier) {
                ((Property.ValueChangeNotifier) parent).removeListener(synchronizer);
            }
        }
    }
}
