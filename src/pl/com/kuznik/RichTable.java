package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class RichTable extends Table {

    private boolean paginated;
    private boolean selectable = true;
    private PageContainerProvider pageProvider;
    private Container originalContainer;
    private Set<DataSourceChangedListener> dataSourceChangeListeners;
    private Paginator paginator;
    private RowHider hider;
    private RowEditor editor;
    private final ControlPanel controlPanel = new ControlPanel();

    public RichTable() {
        // enable collapsing columns by default
        setColumnCollapsingAllowed(true);
        // enable column reordering by default
        setColumnReorderingAllowed(true);
        // enable multiselect by default
        setMultiSelect(true);

        setWriteThrough(false);
        setImmediate(false);

        // enable pagination by default
        setPaginated(true);

        addListener((DataSourceChangedListener) getPaginator());
        addListener((DataSourceChangedListener) getHider());
        addListener((Property.ValueChangeListener) getHider());
        addListener((ItemClickListener) getEditor());

        getPaginator().addListener((PageChangedListener) getHider());
    }

    @Override
    public void attach() {
        super.attach();
        getWindow().addActionHandler(editor);
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

    private RowHider getHider() {
        if (hider == null) {
            hider = new RowHider();
        }
        return hider;
    }

    private RowEditor getEditor() {
        if (editor == null) {
            editor = new RowEditor();
        }
        return editor;
    }

    private PageContainerProvider getPageProvider() {
        if (pageProvider == null) {
            pageProvider = new PageContainerProvider(new IndexedContainer(), 0);
        }
        return pageProvider;
    }

    @Override
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        super.setSelectable(selectable);
    }

    public void setPaginated(boolean paginated) {
        if (this.paginated != paginated) {
            this.paginated = paginated;
            if (paginated) {
                initializePageProvider(getContainerDataSource());
            } else {
                super.setContainerDataSource(originalContainer);
            }
        }
    }

    public boolean isPaginated() {
        return paginated;
    }

    @Override
    public void setContainerDataSource(Container newDataSource) {
        if (isPaginated()) {
            initializePageProvider(newDataSource);
        } else {
            super.setContainerDataSource(newDataSource);
        }
        notifyDataSourceChanged();
    }

    @Override
    public void sort() {
        if (isPaginated()) {
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

    private void initializePageProvider(Container newDataSource) {
        originalContainer = newDataSource;
        if (originalContainer instanceof Container.Indexed) {
            pageProvider = new PageContainerProvider((Container.Indexed) originalContainer,
                    getPaginator().getItemsPerPage());
            super.setContainerDataSource(
                    getPageProvider().getPageContainer(getPaginator().getCurrentPage()));
        } else {
            throw new UnsupportedOperationException("Provided container does not support pagination.");
        }
    }

    private class Paginator
            extends HorizontalLayout
            implements Button.ClickListener, RichTable.DataSourceChangedListener,
            FocusListener, Property.ValueChangeListener, PageChangedListener {

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
        private Set<PageChangedListener> pageChangeListeners = null;

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

            addListener((PageChangedListener) this);
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
            firePageChanged();
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

        public void addListener(PageChangedListener listener) {
            getPageChangeListeners().add(listener);
        }

        private Set<PageChangedListener> getPageChangeListeners() {
            if (pageChangeListeners == null) {
                pageChangeListeners = new HashSet<PageChangedListener>();
            }
            return pageChangeListeners;
        }

        private void firePageChanged() {
            for (PageChangedListener pageChangedListener : getPageChangeListeners()) {
                pageChangedListener.pageChanged();
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
            if (event.getProperty() == itemsPerPageCombo) {
                changeItemsPerPage((Integer) event.getProperty().getValue());
            }
        }

        public void pageChanged() {
            updateUI();
        }
    }

    private class RowHider
            extends HorizontalLayout
            implements Button.ClickListener, RichTable.DataSourceChangedListener,
            Property.ValueChangeListener, PageChangedListener {

        private final String SHOW_BUTTON_LABEL = "Show hidden";
        private Button hideButton = new Button("Hide selected");
        private Button showButton = new Button(SHOW_BUTTON_LABEL);
        private int hiddenRowsCount = 0;

        public RowHider() {
            addComponent(hideButton);
            hideButton.addListener((Button.ClickListener) this);
            addComponent(showButton);
            showButton.addListener((Button.ClickListener) this);
            updateUI();
        }

        public void buttonClick(ClickEvent event) {
            Button source = event.getButton();
            if (source == hideButton) {
                hideSelectedRows();
            } else if (source == showButton) {
                showHiddenRows();
            }
        }

        private void addHiddenRows(int count) {
            hiddenRowsCount += count;
        }

        private String createHiddenRowsText() {
            return areRowsHidden() ? " (" + getHiddenRowsCount() + ")" : "";
        }

        private void resetHiddenRowsCount() {
            hiddenRowsCount = 0;
        }

        private boolean areRowsHidden() {
            return hiddenRowsCount > 0;
        }

        private boolean areRowsSelected() {
            Object value = RichTable.this.getValue();
            if (value == null) {
                return false;
            }
            if (value instanceof Collection) {
                return ((Collection<?>) value).size() > 0;
            } else {
                return false;
            }
        }

        public int getHiddenRowsCount() {
            return hiddenRowsCount;
        }

        private void hideSelectedRows() {
            Object value = RichTable.this.getValue();
            if (value instanceof Collection) {
                Collection<?> rowset = (Collection<?>) value;
                pageProvider.hideRows(rowset);
                addHiddenRows(rowset.size());
            }
            RichTable.this.setValue(null);
            updateUI();
        }

        private void showHiddenRows() {
            if (areRowsHidden()) {
                RichTable.this.disableContentRefreshing();
                pageProvider.showHiddenRows();
                RichTable.this.enableContentRefreshing(true);
                resetHiddenRowsCount();
                updateUI();
            }
        }

        private void updateUI() {
            hideButton.setEnabled(areRowsSelected());
            showButton.setEnabled(areRowsHidden());
            showButton.setCaption(SHOW_BUTTON_LABEL + createHiddenRowsText());
        }

        public void dataSourceChanged() {
            resetHiddenRowsCount();
            updateUI();
        }

        public void valueChange(Property.ValueChangeEvent event) {
            updateUI();
        }

        public void pageChanged() {
            resetHiddenRowsCount();
            updateUI();
        }
    }

    private class RowEditor
            implements ItemClickListener, Handler {

        private Action submit_action = new ShortcutAction("Default", ShortcutAction.KeyCode.ENTER, null);
        private Action discard_action = new ShortcutAction("Escape", ShortcutAction.KeyCode.ESCAPE, null);
        private Object currentlyEditedItem = null;
        private LinkedList<Field> fields = null;

        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                final Object newItemId = event.getItemId();
                if (newItemId != currentlyEditedItem) {
                    discardChanges();
                }
                currentlyEditedItem = newItemId;
                fields = new LinkedList<Field>();
                RichTable.this.setTableFieldFactory(new TableFieldFactory() {

                    public Field createField(Container container, Object itemId,
                            Object propertyId, Component uiContext) {

                        if (itemId == newItemId) {
                            Field field = DefaultFieldFactory.createFieldByPropertyType(
                                    container.getItem(itemId).getItemProperty(propertyId).getType());
                            fields.add(field);
                            field.setWriteThrough(false);
                            field.setPropertyDataSource(container.getItem(itemId).getItemProperty(propertyId));
                            return field;
                        }
                        return null;
                    }
                });
                goToEditMode();
            }
        }

        public Action[] getActions(Object target, Object sender) {
            return new Action[]{submit_action, discard_action};
        }

        public void handleAction(Action action, Object sender, Object target) {
            if (action == submit_action) {
                commitChanges();
                goOutEditMode();
            } else if (action == discard_action) {
                discardChanges();
                goOutEditMode();
            }
        }

        private void goToEditMode() {
            RichTable.this.setValue(null); // ensure notifying all PropertyChangeListeners
            RichTable.super.setSelectable(false); // use super-implementation to avoid overwriting private field
            RichTable.this.setEditable(true);
            getWindow().showNotification("In 'Edit' mode.", "Hit RETURN to accept or ESC to abort.");
        }

        private void goOutEditMode() {
            RichTable.this.setEditable(false);
            RichTable.super.setSelectable(RichTable.this.selectable);
        }

        private void discardChanges() {
            if (currentlyEditedItem == null) {
                return;
            }
            for (Field field : fields) {
                if (field != null) { // non-editable fields can be null
                    field.discard();
                }
            }
            currentlyEditedItem = null;
        }

        private void commitChanges() {
            if (currentlyEditedItem == null) {
                return;
            }
            for (Field field : fields) {
                if (field != null && field.isValid()) {  // non-editable fields can be null
                    field.commit();
                }
            }
            currentlyEditedItem = null;
        }
    }

    public class ControlPanel extends HorizontalLayout {

        ControlPanel() {
            addComponent(getHider());
            addComponent(getPaginator());
        }
    }

    private interface DataSourceChangedListener {

        public void dataSourceChanged();
    }

    private interface PageChangedListener {

        public void pageChanged();
    }
}
