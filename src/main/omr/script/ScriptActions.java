//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S c r i p t A c t i o n s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Book;
import omr.sheet.ui.SheetActions;
import omr.sheet.ui.SheetsController;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;

import omr.util.BasicTask;
import omr.util.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;
import omr.OMR;

/**
 * Class {@code ScriptActions} gathers UI actions related to script handling.
 * These static member classes are ready to be picked by the plugins mechanism.
 *
 * @author Hervé Bitteur
 */
public class ScriptActions
        extends SheetActions
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScriptActions.class);

    /** Singleton. */
    private static ScriptActions INSTANCE;

    /** Default parameter. */
    public static final Param<Boolean> defaultPrompt = new Default();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Singleton.
     */
    private ScriptActions ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // checkStored //
    //-------------//
    /**
     * Check whether the provided script has been safely saved if needed
     * (and therefore, if the sheet can be closed)
     *
     * @param script the script to check
     * @return true if close is allowed, false if not
     */
    public static boolean checkStored (Script script)
    {
        if (script.isModified() && defaultPrompt.getSpecific()) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    "Save script for book " + script.getBook().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Task<Void, Void> task = getInstance().storeScript(null);

                if (task != null) {
                    task.execute();
                }

                // Here user has saved the script
                return true;
            }

            if (answer == JOptionPane.NO_OPTION) {
                // Here user specifically chooses NOT to save the script
                return true;
            }

            // // Here user says Oops!, cancelling the current close request
            return false;
        } else {
            return true;
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized ScriptActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScriptActions();
        }

        return INSTANCE;
    }

    //------------//
    // loadScript //
    //------------//
    @Action
    public Task<Void, Void> loadScript (ActionEvent e)
    {
        final File file = UIUtil.fileChooser(
                false,
                OMR.getGui().getFrame(),
                new File(constants.defaultScriptDirectory.getValue()),
                new OmrFileFilter(
                        "Score script files",
                        new String[]{ScriptManager.SCRIPT_EXTENSION}));

        if (file != null) {
            return new LoadScriptTask(file);
        } else {
            return null;
        }
    }

    //-------------//
    // storeScript //
    //-------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeScript (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final File scriptFile = book.getScriptFile();

        if (scriptFile != null) {
            return new StoreScriptTask(book.getScript(), scriptFile);
        } else {
            return storeScriptAs(e);
        }
    }

    //---------------//
    // storeScriptAs //
    //---------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeScriptAs (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Let the user select a script output file
        File scriptFile = UIUtil.fileChooser(
                true,
                OMR.getGui().getFrame(),
                getDefaultScriptFile(book),
                new OmrFileFilter("Script files", new String[]{ScriptManager.SCRIPT_EXTENSION}));

        if (scriptFile != null) {
            return new StoreScriptTask(book.getScript(), scriptFile);
        } else {
            return null;
        }
    }

    //----------------------//
    // getDefaultScriptFile //
    //----------------------//
    /**
     * Report the default file where the script should be written to
     *
     * @param book the containing book
     * @return the default file for saving the script
     */
    private File getDefaultScriptFile (Book book)
    {
        return (book.getScriptFile() != null) ? book.getScriptFile()
                : new File(
                        constants.defaultScriptDirectory.getValue(),
                        book.getRadix() + ScriptManager.SCRIPT_EXTENSION);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String defaultScriptDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCRIPTS_FOLDER.toString(),
                "Default directory for saved scripts");

        private final Constant.Boolean closeConfirmation = new Constant.Boolean(
                true,
                "Should we ask confirmation for closing a sheet with unsaved script?");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Boolean>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Boolean getSpecific ()
        {
            return constants.closeConfirmation.getValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.closeConfirmation.setValue(specific);
                logger.info(
                        "You will {} be prompted to save script when" + " closing score",
                        specific ? "now" : "no longer");

                return true;
            } else {
                return false;
            }
        }
    }

    //----------------//
    // LoadScriptTask //
    //----------------//
    private static class LoadScriptTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        LoadScriptTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            // Actually run the script
            logger.info("Running script file {} ...", file);

            try (FileInputStream is = new FileInputStream(file)) {
                final Script script = ScriptManager.getInstance().load(is);

                if (logger.isDebugEnabled()) {
                    script.dump();
                }

                // Remember (even across runs) the parent directory
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.run();
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file {}", file);
            } catch (IOException ex) {
                logger.warn("Error reading script file {}", file);
            }

            return null;
        }
    }

    //-----------------//
    // StoreScriptTask //
    //-----------------//
    private static class StoreScriptTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Script script;

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        StoreScriptTask (Script script,
                         File file)
        {
            this.script = script;
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            FileOutputStream fos = null;

            try {
                File folder = new File(file.getParent());

                if (folder.mkdirs()) {
                    logger.info("Creating folder {}", folder);
                }

                fos = new FileOutputStream(file);
                omr.script.ScriptManager.getInstance().store(script, fos);
                logger.info("Script stored as {}", file);
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.getBook().setScriptFile(file);
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file " + file + ", " + ex, ex);
            } catch (JAXBException ex) {
                logger.warn("Cannot marshal script, " + ex, ex);
            } catch (Throwable ex) {
                logger.warn("Error storing script, " + ex, ex);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            return null;
        }
    }
}
