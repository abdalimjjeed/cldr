package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import java.util.EnumSet;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.VettingViewer.VoteStatus;

/** Also see {@link org.unicode.cldr.unittest.TestUtilities} */
class TestVettingViewer {
    @Test
    void testDashboardEnglishChanged() {
        if (true) return; // Temporarily disable test

        final String loc = "de";
        final CLDRLocale locale = CLDRLocale.getInstance(loc);
        final PathHeader.Factory phf = PathHeader.getFactory();
        final VoterInfoList vil = new VoterInfoList();
        VettingViewer<Organization> vv =
                new VettingViewer<>(
                        SupplementalDataInfo.getInstance(),
                        CLDRConfig.getInstance().getCldrFactory(),
                        new VettingViewer.UsersChoice<Organization>() {

                            @Override
                            public String getWinningValueForUsersOrganization(
                                    CLDRFile cldrFile, String path, Organization user) {
                                return null;
                            }

                            @Override
                            public VoteStatus getStatusForUsersOrganization(
                                    CLDRFile cldrFile, String path, Organization user) {
                                return VoteStatus.losing;
                            }

                            @Override
                            public boolean userDidVote(int userId, CLDRLocale loc, String path) {
                                return false;
                            }

                            @Override
                            public VoteType getUserVoteType(
                                    int userId, CLDRLocale loc, String path) {
                                return VoteType.NONE;
                            }

                            @Override
                            public VoteResolver<String> getVoteResolver(
                                    CLDRFile cldrFile, final CLDRLocale loc, final String path) {
                                VoteResolver<String> r = new VoteResolver<>(vil);
                                r.setLocale(locale, getPathHeader(path));
                                return r;
                            }

                            protected final PathHeader getPathHeader(String xpath) {
                                return phf.fromPath(xpath);
                            }
                        });

        final Factory baselineFactory = CLDRConfig.getInstance().getCldrFactory();
        final Factory sourceFactory = baselineFactory;

        EnumSet<NotificationCategory> choiceSet = EnumSet.of(NotificationCategory.englishChanged);
        CLDRFile sourceFile = sourceFactory.make(loc, true);
        CLDRFile baselineFile = baselineFactory.make(loc, true);
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted;
        VettingParameters args = new VettingParameters(choiceSet, locale, Level.MODERN);
        args.setUserAndOrganization(0 /* userId */, Organization.surveytool);
        args.setFiles(sourceFile, baselineFile);
        VettingViewer<Organization>.DashboardData dd = vv.generateDashboard(args);
        boolean foundAny = false;
        for (Entry<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> e :
                dd.sorted.entrySet()) {
            for (NotificationCategory problem : e.getValue().problems) {
                if (problem.name().equals("englishChanged")) {
                    foundAny = true;
                    final String path = e.getValue().codeOutput.getOriginalPath();
                    final String previous =
                            VettingViewer.getOutdatedPaths().getPreviousEnglish(path);
                    //// For dumping a full list
                    // if(previous.equals(OutdatedPaths.NO_VALUE)) {
                    //     System.out.println(path);
                    // }
                    assertNotEquals(
                            OutdatedPaths.NO_VALUE,
                            previous,
                            "expected prev english to be present for " + path);
                }
            }
        }
        // The next line will mark this test as *skipped* if no English Changed were found,
        // but won't fail the test.
        assumeTrue(foundAny, "Did not find any English Changed. May need to revamp the test.");
    }
}
