package de.marhali.easyi18n.ui.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import de.marhali.easyi18n.service.DataStore;
import de.marhali.easyi18n.model.LocalizedNode;
import de.marhali.easyi18n.model.DataSynchronizer;
import de.marhali.easyi18n.model.Translations;
import de.marhali.easyi18n.model.KeyedTranslation;
import de.marhali.easyi18n.model.TranslationDelete;
import de.marhali.easyi18n.model.table.TableModelTranslator;
import de.marhali.easyi18n.ui.dialog.EditDialog;
import de.marhali.easyi18n.ui.listener.DeleteKeyListener;
import de.marhali.easyi18n.ui.listener.PopupClickListener;
import de.marhali.easyi18n.ui.renderer.TableRenderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

/**
 * Shows translation state as table.
 * @author marhali
 */
public class TableView implements DataSynchronizer {

    private final Project project;

    private JPanel rootPanel;
    private JPanel containerPanel;

    private JBTable table;

    public TableView(Project project) {
        this.project = project;

        table = new JBTable();
        table.getEmptyText().setText(ResourceBundle.getBundle("messages").getString("view.empty"));
        table.addMouseListener(new PopupClickListener(this::handlePopup));
        table.addKeyListener(new DeleteKeyListener(handleDeleteKey()));
        table.setDefaultRenderer(String.class, new TableRenderer());

        containerPanel.add(new JBScrollPane(table));
    }

    private void handlePopup(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());

        if(row >= 0) {
            String fullPath = String.valueOf(table.getValueAt(row, 0));
            LocalizedNode node = DataStore.getInstance(project).getTranslations().getNode(fullPath);

            if(node != null) {
                new EditDialog(project, new KeyedTranslation(fullPath, node.getValue())).showAndHandle();
            }
        }
    }

    private Runnable handleDeleteKey() {
        return () -> {
            for (int selectedRow : table.getSelectedRows()) {
                String fullPath = String.valueOf(table.getValueAt(selectedRow, 0));

                DataStore.getInstance(project).processUpdate(
                        new TranslationDelete(new KeyedTranslation(fullPath, null)));
            }
        };
    }

    @Override
    public void synchronize(@NotNull Translations translations, @Nullable String searchQuery) {
        table.setModel(new TableModelTranslator(translations, searchQuery, update ->
                DataStore.getInstance(project).processUpdate(update)));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public JBTable getTable() {
        return table;
    }
}