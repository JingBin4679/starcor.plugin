package com.starcor.plugin.action;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateDirectoryOrPackageAction;
import com.intellij.ide.actions.CreateDirectoryOrPackageHandler;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.starcor.plugin.StarcorPluginIcons;

public class StarcorCreatePluginFileAction extends CreateDirectoryOrPackageAction {

    public static final String NEW_STARCOR_PLUGIN_FILE = "New Starcor Plugin File";
    private static final String FILE_TEMPLATE = "Starcor.Plugin";
    public static final String PLUGIN_PROTOCOL = "plugin_protocol.xml";

    public StarcorCreatePluginFileAction() {
        super();
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        IdeView ideView = anActionEvent.getData(LangDataKeys.IDE_VIEW);
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (ideView != null && project != null) {
            PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(ideView);
            if (directory != null) {
                boolean isPackage = !PsiDirectoryFactory.getInstance(project).isPackage(directory);
                CreateDirectoryOrPackageHandler handler = new CreateDirectoryOrPackageHandler(project, directory, isPackage, isPackage ? "\\/" : ".");
                Messages.showInputDialog(project, "Enter new plugin name:", NEW_STARCOR_PLUGIN_FILE, Messages.getQuestionIcon(), "", handler);
                PsiDirectory psiDirectory = (PsiDirectory) handler.getCreatedElement();
                genJavaCode(project, psiDirectory);

                genPluginProtocolFile(project, psiDirectory);

            }
        }
    }

    /**
     * 生成Java代码
     * @param psiDirectory
     */
    private void genJavaCode(Project project, PsiDirectory psiDirectory) {
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                PsiClass psiClass = JavaDirectoryService.getInstance().createClass(psiDirectory, "Plugin");
                PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                PsiField field = factory.createField("PLUGIN_VERSION", PsiType.INT);
                field.getModifierList().setModifierProperty(PsiModifier.PUBLIC,true);
            }
        });
    }

    /**
     * 生成协议文件
     * @param project
     * @param psiDirectory
     */
    private void genPluginProtocolFile(Project project, PsiDirectory psiDirectory) {
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                String pluginName = psiDirectory.getName();
                PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(project.getBaseDir());
                PsiDirectory pluginsDir = baseDir.findSubdirectory("plugins");
                if (pluginsDir == null) {

                    pluginsDir = baseDir.createSubdirectory("plugins");
                }
                PsiDirectory pluginProtocolDir = pluginsDir.findSubdirectory(pluginName);
                if (pluginProtocolDir == null) {
                    pluginProtocolDir = pluginsDir.createSubdirectory(pluginName);
                }
                PsiFile file = pluginProtocolDir.findFile(PLUGIN_PROTOCOL);
                if (file == null) {
                    XmlFile xmlFile = (XmlFile) PsiFileFactory.getInstance(project).createFileFromText(PLUGIN_PROTOCOL, StdFileTypes.XML, "<plugin ");
                    XmlDocument document = xmlFile.getDocument();
                    if (document != null && document.getRootTag() != null) {
                        XmlTag rootTag = document.getRootTag();
                        rootTag.setName("plugin");
                        rootTag.setAttribute("id", "plugin://" + pluginName);
                        rootTag.setAttribute("name", pluginName);
                        rootTag.setAttribute("version", "1");
                        PsiFile psiFile = pluginProtocolDir.findFile(PLUGIN_PROTOCOL);
                        if (psiFile != null) {
                            psiFile.delete();
                        }
                        pluginProtocolDir.add(xmlFile);
                    }
                }
            }
        });
    }


    @Override
    public void update(AnActionEvent anActionEvent) {
        Presentation presentation = anActionEvent.getPresentation();
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            presentation.setVisible(false);
            presentation.setEnabled(false);
        } else {
            IdeView ideView = anActionEvent.getData(LangDataKeys.IDE_VIEW);
            if (ideView == null) {
                presentation.setVisible(false);
                presentation.setEnabled(false);
            } else {
                PsiDirectory[] directories = ideView.getDirectories();
                if (directories.length == 0) {
                    presentation.setVisible(false);
                    presentation.setEnabled(false);
                } else {
                    presentation.setVisible(true);
                    presentation.setEnabled(true);
                    boolean isPackage = false;
                    PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(project);
                    PsiDirectory[] dirs = directories;
                    int length = directories.length;

                    for (int i = 0; i < length; ++i) {
                        PsiDirectory dir = dirs[i];
                        if (factory.isPackage(dir)) {
                            isPackage = true;
                            break;
                        }
                    }

                    VirtualFile data = anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
                    boolean show = data != null && "plugin".equals(data.getName());
                    if (isPackage && show) {
                        presentation.setVisible(true);
                        presentation.setEnabled(true);
                        presentation.setText(NEW_STARCOR_PLUGIN_FILE);
                        presentation.setIcon(StarcorPluginIcons.ICON);
                    } else {
                        presentation.setVisible(false);
                        presentation.setEnabled(false);
                    }

                }
            }
        }
    }
}
