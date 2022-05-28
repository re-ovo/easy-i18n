package de.marhali.easyi18n.tabs.mapper;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.JBColor;

import de.marhali.easyi18n.model.TranslationData;
import de.marhali.easyi18n.model.TranslationNode;
import de.marhali.easyi18n.model.KeyPath;
import de.marhali.easyi18n.settings.ProjectSettings;
import de.marhali.easyi18n.util.UiUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapping {@link TranslationData} to {@link TreeModel}.
 * @author marhali
 */
public class TreeModelMapper extends DefaultTreeModel {

    private final TranslationData data;
    private final ProjectSettings state;

    public TreeModelMapper(TranslationData data, ProjectSettings state) {
        super(null);

        this.data = data;
        this.state = state;

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        this.generateNodes(rootNode, this.data.getRootNode());
        super.setRoot(rootNode);
    }

    /**
     * @param parent Parent tree node
     * @param translationNode Layer of translation node to write to tree
     * @return true if children nodes misses any translation values
     */
    private boolean generateNodes(@NotNull DefaultMutableTreeNode parent, @NotNull TranslationNode translationNode) {
        boolean foundMissing = false;

        for(Map.Entry<String, TranslationNode> entry : translationNode.getChildren().entrySet()) {
            String key = entry.getKey();
            TranslationNode childTranslationNode = entry.getValue();

            if(!childTranslationNode.isLeaf()) {
                // Nested node - run recursively
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(key);

                if(this.generateNodes(childNode, childTranslationNode)) { // Mark red if any children misses translations
                    PresentationData data = new PresentationData(key, null, null, null);
                    data.setForcedTextForeground(JBColor.RED);
                    childNode.setUserObject(data);
                    foundMissing = true;
                }

                parent.add(childNode);

            } else {
                String previewLocale = this.state.getPreviewLocale();
                String sub = "(" + previewLocale + ": " + childTranslationNode.getValue().get(previewLocale) + ")";
                String tooltip = UiUtil.generateHtmlTooltip(childTranslationNode.getValue().getEntries());

                PresentationData data = new PresentationData(key, sub, null, null);
                data.setTooltip(tooltip);

                if(childTranslationNode.getValue().size() != this.data.getLocales().size()) {
                    data.setForcedTextForeground(JBColor.RED);
                    foundMissing = true;
                }

                parent.add(new DefaultMutableTreeNode(data));
            }
        }

        return foundMissing;
    }

    /**
     * Converts KeyPath to TreePath
     * @param fullPath Absolute translation key path
     * @return Converted TreePath
     */
    public @NotNull TreePath findTreePath(@NotNull KeyPath fullPath) {
        List<Object> nodes = new ArrayList<>();

        TreeNode currentNode = (TreeNode) this.getRoot();
        nodes.add(currentNode);

        for(String section : fullPath) {
            currentNode = this.findNode(currentNode, section);

            if(currentNode == null) {
                break;
            }

            nodes.add(currentNode);
        }

        return new TreePath(nodes.toArray());
    }

    private @Nullable DefaultMutableTreeNode findNode(@NotNull TreeNode parent, @NotNull String key) {
        for(int i = 0; i < parent.getChildCount(); i++) {
            TreeNode child = parent.getChildAt(i);

            if(child instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode mutableChild = (DefaultMutableTreeNode) child;
                String childKey = mutableChild.getUserObject().toString();

                if(mutableChild.getUserObject() instanceof PresentationData) {
                    childKey = ((PresentationData) mutableChild.getUserObject()).getPresentableText();
                }

                if(childKey != null && childKey.equals(key)) {
                    return mutableChild;
                }
            }
        }

        return null;
    }
}