package pl.com.kuznik;

import com.vaadin.data.Container;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import java.util.LinkedHashMap;

/**
 *
 * @author Krzysztof Kuźnik <kmkuznik at gmail.com>
 */
public class RichTable extends Table {

    private boolean paginated = true;
    private int itemsPerPage = 15;
    private Paginator paginator = new Paginator();
    private final ControlPanel controlPanel = new ControlPanel();

    public RichTable() {
        // enable collapsing columns by default
        setColumnCollapsingAllowed(true);
        // enable column reordering by default
        setColumnReorderingAllowed(true);
        // enable multiselect by default
        setMultiSelect(true);
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public Component getPaginator() {
        return paginator;
    }
    private PageContainerProvider pageProvider;

    @Override
    public void setContainerDataSource(Container newDataSource) {
        // TODO check instanceof Container Indexed
        pageProvider = new PageContainerProvider((Container.Indexed) newDataSource, itemsPerPage);
        super.setContainerDataSource(pageProvider.getPageContainer(1));
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

    private class Paginator extends HorizontalLayout implements Button.ClickListener {

        private Button firstPageButton = new Button("<<");
        private Button previousPageButton = new Button("<");
        private Button nextPageButton = new Button(">");
        private Button lastPageButton = new Button(">>");
        private TextField pageNumberText = new TextField();
        private Button goToPageButton = new Button("GO");
        private int currentPage = 1;

        public Paginator() {
            addComponent(firstPageButton);
            firstPageButton.addListener((Button.ClickListener) this);
            addComponent(previousPageButton);
            previousPageButton.addListener((Button.ClickListener) this);
            addComponent(nextPageButton);
            nextPageButton.addListener((Button.ClickListener) this);
            addComponent(lastPageButton);
            lastPageButton.addListener((Button.ClickListener) this);

            addComponent(pageNumberText);
            pageNumberText.setValue(getCurrentPageText());
            addComponent(goToPageButton);
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
            }
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
            RichTable.super.setContainerDataSource(pageProvider.getPageContainer(currentPage));
            restoreColumnsState(columnsState);
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
            return currentPage + " / " + pageProvider.getLastPageNumber();
        }

    }

    public class ControlPanel extends HorizontalLayout {

        private Button hideButton = new Button("Hide");

        ControlPanel() {
            addComponent(hideButton);
            addComponent(getPaginator());
        }
    }
}
