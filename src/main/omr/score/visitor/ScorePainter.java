//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e P a i n t e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MeasureNode;
import omr.score.Note;
import omr.score.PartNode;
import omr.score.Score;
import static omr.score.ScoreConstants.*;
import omr.score.ScoreNode;
import omr.score.ScorePoint;
import omr.score.Slot;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.SystemPoint;
import omr.score.TimeSignature;
import omr.score.UnitDimension;

import omr.ui.icon.IconManager;
import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Class <code>ScorePainter</code> defines for every node in Score hierarchy
 * the painting of node in the <b>Score</b> display.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePainter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScorePainter.class);

    /** Brace icon */
    private static final SymbolIcon icon = IconManager.getInstance()
                                                      .loadSymbolIcon("BRACE");

    /** Stroke to draw beams */
    private static final Stroke beamStroke = new BasicStroke(4f);

    /** Sequence of colors for beams */
    private static Color[] beamColors = new Color[] {
                                            Color.RED, Color.CYAN, Color.ORANGE,
                                            Color.GREEN
                                        };

    /** Color for slot axis */
    private static final Color slotColor = new Color(
        0,
        0,
        0,
        constants.slotAlpha.getValue());

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics2D g;

    /** Display zoom */
    private final Zoom zoom;

    /** Used for icon image transformation */
    private final AffineTransform transform = new AffineTransform();

    /** Index for beam color */
    private int beamIndex;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScorePainter //
    //--------------//
    /**
     * Creates a new ScorePainter object.
     *
     * @param g Graphic context
     * @param z zoom factor
     */
    public ScorePainter (Graphics g,
                         Zoom     z)
    {
        this.g = (Graphics2D) g;
        this.zoom = z;

        this.g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Barline //
    //---------------//
    public boolean visit (Barline barline)
    {
        Shape shape = barline.getShape();

        if (shape != null) {
            // Draw the barline symbol for each stave in the measure
            SystemPart part = barline.getPart();

            for (TreeNode node : part.getStaves()) {
                Staff staff = (Staff) node;
                part.paintSymbol(g, zoom, shape, barline.getCenter(), staff, 0);
            }
        } else {
            logger.warning("No shape for barline " + this);
        }

        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    public boolean visit (Beam beam)
    {
        Stroke oldStroke = g.getStroke();
        g.setStroke(beamStroke);

        // Choose beam color;
        beamIndex = (beamIndex + 1) % beamColors.length;
        //g.setColor(beamColors[beamIndex]);
        g.setColor(Color.black);

        // Draw the beam line
        drawLine(beam, beam.getLeft(), beam.getRight());
        g.setStroke(oldStroke);

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    public boolean visit (Chord chord)
    {
        if (chord.getStem() != null) {
            g.setColor(Color.black);
            drawLine(chord, chord.getTailLocation(), chord.getHeadLocation());
        }

        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    public boolean visit (Clef clef)
    {
        // Draw the clef symbol
        clef.getPart()
            .paintSymbol(
            g,
            zoom,
            clef.getShape(),
            clef.getCenter(),
            clef.getStaff(),
            clef.getPitchPosition());

        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    public boolean visit (KeySignature keySignature)
    {
        if (keySignature.getPitchPosition() != null) {
            keySignature.getPart()
                        .paintSymbol(
                g,
                zoom,
                keySignature.getShape(),
                keySignature.getCenter(),
                keySignature.getStaff(),
                keySignature.getPitchPosition());
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    public boolean visit (Measure measure)
    {
        SystemPart part = measure.getPart();

        // Write the measure id, on the first staff  of the first part only
        if (part.getId() == 1) {
            ScorePoint staffOrigin = measure.getPart()
                                            .getFirstStaff()
                                            .getDisplayOrigin();
            g.setColor(Color.lightGray);
            g.drawString(
                Integer.toString(measure.getId()),
                zoom.scaled(staffOrigin.x + measure.getLeftX()) - 5,
                zoom.scaled(staffOrigin.y) - 15);
        }

        // Draw slot vertical lines ?
        if (constants.slotPainting.getValue()) {
            g.setColor(slotColor);

            for (Slot slot : measure.getSlots()) {
                // Draw vertical line using slot abscissa
                ScorePoint partOrigin = measure.getPart()
                                               .getFirstStaff()
                                               .getDisplayOrigin();
                int        x = zoom.scaled(partOrigin.x + slot.getX());
                g.drawLine(
                    x,
                    zoom.scaled(partOrigin.y),
                    x,
                    zoom.scaled(
                        measure.getPart().getLastStaff().getDisplayOrigin().y +
                        STAFF_HEIGHT));
            }
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    public boolean visit (Note note)
    {
        // If note is attached to a stem, link the note display to the stem one
        Chord chord = note.getChord();
        Glyph stem = chord.getStem();

        if (stem != null) {
            note.getPart()
                .paintSymbol(g, zoom, note.getShape(), note.getCenter(), chord);
        } else {
            Shape shape = note.getShape();
            Shape displayShape;

            if (shape == Shape.MULTI_REST) {
                displayShape = Shape.MULTI_REST_DISPLAY;
            } else if ((shape == Shape.WHOLE_REST) ||
                       (shape == Shape.HALF_REST)) {
                displayShape = Shape.HALF_OR_WHOLE_REST_DISPLAY;
            } else {
                displayShape = shape;
            }

            note.getPart()
                .paintSymbol(g, zoom, displayShape, note.getCenter());
        }

        return true;
    }

    //-----------------//
    // visit ScoreNode //
    //-----------------//
    public boolean visit (ScoreNode musicNode)
    {
        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    //------------//
    // visit Slur //
    //------------//
    public boolean visit (Slur slur)
    {
        slur.getArc()
            .draw(g, slur.getDisplayOrigin(), zoom);

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    public boolean visit (Staff staff)
    {
        Point origin = staff.getDisplayOrigin();
        g.setColor(Color.black);

        // Draw the staff lines
        for (int i = 0; i < LINE_NB; i++) {
            // Y of this staff line
            int y = zoom.scaled(origin.y + (i * INTER_LINE));
            g.drawLine(
                zoom.scaled(origin.x),
                y,
                zoom.scaled(origin.x + staff.getWidth()),
                y);
        }

        return true;
    }

    //-------------------//
    // visit MeasureNode //
    //-------------------//
    public boolean visit (MeasureNode measureNode)
    {
        return true;
    }

    //----------------//
    // visit PartNode //
    //----------------//
    public boolean visit (PartNode node)
    {
        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    public boolean visit (System system)
    {
        // Check whether our system is impacted)
        Rectangle clip = g.getClipBounds();
        int       xMargin = INTER_SYSTEM;
        int       systemLeft = system.getRightPosition() + xMargin;
        int       systemRight = system.getDisplayOrigin().x - xMargin;

        if ((zoom.unscaled(clip.x) > systemLeft) ||
            (zoom.unscaled(clip.x + clip.width) < systemRight)) {
            return false;
        } else {
            UnitDimension dimension = system.getDimension();
            Point         origin = system.getDisplayOrigin();
            g.setColor(Color.lightGray);

            // Draw the system left edge
            g.drawLine(
                zoom.scaled(origin.x),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            // Draw the system right edge
            g.drawLine(
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            return true;
        }
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    public boolean visit (SystemPart part)
    {
        // Draw a brace if there is more than one stave in the part
        if (part.getStaves()
                .size() > 1) {
            // Top & bottom of brace to draw
            int        top = part.getFirstStaff()
                                 .getDisplayOrigin().y;
            int        bot = part.getLastStaff()
                                 .getDisplayOrigin().y + STAFF_HEIGHT;
            double     height = zoom.scaled(bot - top + 1);

            // Vertical ratio to extend the icon */
            double     ratio = height / icon.getIconHeight();

            // Offset on left of system
            int        dx = 10;

            Graphics2D g2 = (Graphics2D) g;
            g.setColor(Color.black);
            transform.setTransform(
                1,
                0,
                0,
                ratio,
                zoom.scaled(part.getSystem()
                                .getDisplayOrigin().x) - dx,
                zoom.scaled(top));
            g2.drawRenderedImage(icon.getImage(), transform);
        }

        // Draw the starting barline, if any
        if (part.getStartingBarline() != null) {
            part.getStartingBarline()
                .accept(this);
        }

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    public boolean visit (TimeSignature timeSignature)
    {
        Shape      shape = timeSignature.getShape();
        SystemPart part = timeSignature.getPart();

        if (shape != null) {
            switch (shape) {
            // If this is an illegal shape, do not draw anything.
            // TBD: we could draw a special sign for this
            case NO_LEGAL_SHAPE :
                break;

            // Is it a complete (one-symbol) time signature ?
            case TIME_FOUR_FOUR :
            case TIME_TWO_TWO :
            case TIME_TWO_FOUR :
            case TIME_THREE_FOUR :
            case TIME_SIX_EIGHT :
            case COMMON_TIME :
            case CUT_TIME :
                part.paintSymbol(g, zoom, shape, timeSignature.getCenter());

                break;
            }
        } else {
            // Assume a (legal) multi-symbol signature
            for (Glyph glyph : timeSignature.getGlyphs()) {
                Shape s = glyph.getShape();

                if (s != null) {
                    SystemPoint center = timeSignature.computeGlyphCenter(
                        glyph);
                    Staff       staff = part.getStaffAt(center);
                    int         pitch = staff.unitToPitch(center.y);
                    part.paintSymbol(g, zoom, s, center, staff, pitch);
                }
            }
        }

        return true;
    }

    private void drawLine (PartNode    node,
                           SystemPoint from,
                           SystemPoint to)
    {
        ScorePoint origin = node.getDisplayOrigin();
        g.drawLine(
            zoom.scaled(origin.x + from.x),
            zoom.scaled(origin.y + from.y),
            zoom.scaled(origin.x + to.x),
            zoom.scaled(origin.y + to.y));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Alpha parameter for slot axis transparency (0 .. 255) */
        Constant.Integer slotAlpha = new Constant.Integer(
            20,
            "Alpha parameter for slot axis transparency (0 .. 255)");

        /** Should the slot be painted */
        Constant.Boolean slotPainting = new Constant.Boolean(
            true,
            "Should the slot be painted");
    }
}
