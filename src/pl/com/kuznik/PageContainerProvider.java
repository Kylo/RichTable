package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;

/**
 *
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class PageContainerProvider {

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
        IndexedContainer container = new IndexedContainer();
        for (Object object : dataContainer.getContainerPropertyIds()) {
            container.addContainerProperty(object, dataContainer.getType(object), null);
        }
        copyItemsToContainer(container, firstRowIdx(currentPageNumber), lastRowIdx(currentPageNumber));
        return container;
    }

    private int firstRowIdx(int page) {
        return page * itemsPerPage;
    }

    private int lastRowIdx(int page) {
        return Math.min((page + 1) * itemsPerPage, dataContainer.size());
    }

    private void copyItemsToContainer(IndexedContainer container, int start, int stop) {
        for (int i = start; i < stop; ++i) {
            Object id = dataContainer.getIdByIndex(i);
            copyItemToContainer(id, container);
        }
    }

    private void copyItemToContainer(Object itemId, IndexedContainer container) {
        Item realItem = dataContainer.getItem(itemId);
        Item shownItem = container.addItem(itemId);
        for (Object property : dataContainer.getContainerPropertyIds()) {
            shownItem.getItemProperty(property).setValue(
                    realItem.getItemProperty(property).getValue());
        }
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
}
