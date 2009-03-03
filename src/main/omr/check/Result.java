//----------------------------------------------------------------------------//
//                                                                            //
//                                R e s u l t                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;


/**
 * Class <code>Result</code> is the root of all result information stored while
 * processing processing checks.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Result
{
    //~ Instance fields --------------------------------------------------------

    /**
     * A readable comment about the result.
     */
    public final String comment;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Result //
    //--------//
    /**
     * Creates a new Result object.
     *
     * @param comment A description of this result
     */
    public Result (String comment)
    {
        this.comment = comment;
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    /**
     * Report a description of this result
     *
     * @return A descriptive string
     */
    @Override
    public String toString ()
    {
        return comment;
    }
}
