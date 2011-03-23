package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 *
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class RichTable extends Table {

    private boolean paginated = true;
    private PageContainerProvider pageProvider;
    private Container originalContainer;
    private Set<DataSourceChangedListener> dataSourceChangeListeners;
    private Paginator paginator;
    private final ControlPanel controlPanel = new ControlPanel();

    public RichTable() {
        // enable collapsing columns by default
        setColumnCollapsingAllowed(true);
        // enable column reordering by default
        setColumnReorderingAllowed(true);
        // enable multiselect by default
        setMultiSelect(true);

        // TODO check if table is paginated
        addListener((DataSourceChangedListener) paginator);
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    private Paginator getPaginator() {
        if (paginator == null) {
            paginator = new Paginator();
        }
        return paginator;
    }

    private PageContainerProvider getPageProvider() {
        if (pageProvider == null) {
            pageProvider = new PageContainerProvider(new IndexedContainer(), 0);
        }
        return pageProvider;
    }

    @Override
    public void setContainerDataSource(Container newDataSource) {
        if (paginated) {
            // TODO check instanceof Container Indexed
            originalContainer = newDataSource;
            pageProvider = new PageContainerProvider((Container.Indexed) newDataSource, getPaginator().getItemsPerPage());
            super.setContainerDataSource(pageProvider.getPageContainer(getPaginator().getCurrentPage()));
            notifyDataSourceChanged();
        } else {
            super.setContainerDataSource(newDataSource);
        }
    }

    @Override
    public void sort() {
        if (paginated) {
            getPageProvider().sort(getSortContainerPropertyId(), isSortAscending());
            notifyDataSourceChanged();
        } else {
            super.sort();
        }
    }

    private LinkedHashMap<Object, Boolean> getColumnsState() {
        LinkedHashMap<Object, Boolean> result = new LinkedHashMap<Object, Boolean>();
        Object[] order = getVisibleColumns();
        for (Object column : order) {
            result.put(column, isColumnCollapsed(column));
        }
        return result;
    }

    private void restoreColumnsState(LinkedHashMap<Object, Boolean> columnsState) {
        setVisibleColumns(columnsState.keySet().toArray());
        for (Object column : columnsState.keySet()) {
            setColumnCollapsed(column, columnsState.get(column));
        }
    }

    private Set<DataSourceChangedListener> getDataSourceChangeListeners() {
        if (dataSourceChangeListeners == null) {
            dataSourceChangeListeners = new HashSet<DataSourceChangedListener>();
        }
        return dataSourceChangeListeners;
    }

    private void addListener(DataSourceChangedListener listener) {
        getDataSourceChangeListeners().add(listener);
    }

    private void notifyDataSourceChanged() {
        for (DataSourceChangedListener listener : getDataSourceChangeListeners()) {
            listener.dataSourceChanged();
        }
    }

    private class Paginator
            extends HorizontalLayout
            implements Button.ClickListener, RichTable.DataSourceChangedListener,
            FocusListener, Property.ValueChangeListener {

        private ComboBox itemsPerPageCombo = new ComboBox();
        private Button firstPageButton = new Button("<<");
        private Button previousPageButton = new Button("<");
        private Button nextPageButton = new Button(">");
        private Button lastPageButton = new Button(">>");
        private TextField pageNumberText = new TextField();
        private Button goToPageButton = new Button("GO");
        private int currentPage = 1;
        private int itemsPerPage;
        private final Integer[] itemsPerPageValues = {10, 20, 30, 50, 100, 200, 300, 500, 1000};

        public Paginator() {
            itemsPerPage = itemsPerPageValues[0];

            addComponent(itemsPerPageCombo);
            itemsPerPageCombo.setNullSelectionAllowed(false);
            for (Integer number : itemsPerPageValues) {
                itemsPerPageCombo.addItem(number);
            }
            itemsPerPageCombo.setImmediate(true);

            addComponent(firstPageButton);
            firstPageButton.addListener((Button.ClickListener) this);
            addComponent(previousPageButton);
            previousPageButton.addListener((Button.ClickListener) this);
            addComponent(nextPageButton);
            nextPageButton.addListener((Button.ClickListener) this);
            addComponent(lastPageButton);
            lastPageButton.addListener((Button.ClickListener) this);

            addComponent(pageNumberText);
            pageNumberText.addListener((FocusListener) this);

            addComponent(goToPageButton);
            goToPageButton.addListener((Button.ClickListener) this);
            updateUI();
        }

        private void updateUI() {
            itemsPerPageCombo.setValue(itemsPerPage);
            itemsPerPageCombo.addListener((Property.ValueChangeListener) this);

            pageNumberText.setValue(getCurrentPageText());

            boolean isFirstPage = (currentPage == 1);
            firstPageButton.setEnabled(!isFirstPage);
            previousPageButton.setEnabled(!isFirstPage);

            boolean isLastPage = (currentPage == pageProvider.getLastPageNumber());
            lastPageButton.setEnabled(!isLastPage);
            nextPageButton.setEnabled(!isLastPage);
        }

        public int getItemsPerPage() {
            return itemsPerPage;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        private void changeItemsPerPage(int newItemsPerPage) {
            if (newItemsPerPage == itemsPerPage) {
                return;
            }
            itemsPerPage = newItemsPerPage;
            currentPage = 1;
            LinkedHashMap<Object, Boolean> columnsState = getColumnsState();
            setContainerDataSource(originalContainer);
            restoreColumnsState(columnsState);
            updateUI();
        }

        // <editor-fold defaultstate="collapsed" desc="Page manipulation">
        private void setPage(int page) {
            if (page < 1) {
                currentPage = 1;
            } else if (page > pageProvider.getLastPageNumber()) {
                currentPage = pageProvider.getLastPageNumber();
            } else {
                currentPage = page;
            }
            LinkedHashMap<Object, Boolean> columnsState = getColumnsState();
            RichTable.super.setContainerDataSource(getPageProvider().getPageContainer(currentPage));
            restoreColumnsState(columnsState);
            updateUI();
        }

        private void goToNextPage() {
            setPage(currentPage + 1);
        }

        private void goToPreviousPage() {
            setPage(currentPage - 1);
        }

        private void goToLastPage() {
            setPage(pageProvider.getLastPageNumber());
        }

        private void goToFirstPage() {
            setPage(1);
        }
        // </editor-fold>

        private String getCurrentPageText() {
            return currentPage + " / " + getPageProvider().getLastPageNumber();
        }

        private int getCurrentPageTextLength() {
            return Integer.toString(currentPage).length();
        }

        private int parsePageToGoText(String textPageNumber) {
            try {
                return Integer.parseInt(textPageNumber.trim().split("[^0-9]")[0].trim());
            } catch (NumberFormatException ex) {
                return currentPage;
            }
        }

        public void buttonClick(ClickEvent event) {
            final Button source = event.getButton();
            if (source == firstPageButton) {
                goToFirstPage();
            } else if (source == previousPageButton) {
                goToPreviousPage();
            } else if (source == nextPageButton) {
                goToNextPage();
            } else if (source == lastPageButton) {
                goToLastPage();
            } else if (source == goToPageButton) {
                setPage(parsePageToGoText(pageNumberText.toString()));
            }
        }

        public void dataSourceChanged() {
            setPage(currentPage);
            updateUI();
        }

        public void focus(FocusEvent event) {
            Component source = event.getComponent();
            if (source == pageNumberText) {
                pageNumberText.setSelectionRange(0, getCurrentPageTextLength());
            }
        }

        public void valueChange(Property.ValueChangeEvent event) {
            changeItemsPerPage((Integer) event.getProperty().getValue());
        }
    }

    public class ControlPanel extends HorizontalLayout {

        private Button hideButton = new Button("Hide");

        ControlPanel() {
            addComponent(hideButton);
            addComponent(getPaginator());
        }
    }

    private interface DataSourceChangedListener {

        public void dataSourceChanged();
    }
}
