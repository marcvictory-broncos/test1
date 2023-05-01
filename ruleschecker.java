private List<ValidationResult> getValidatedContentByRules(String contentTobeValidated, AppConstants.CampaignValidationResult
            campaignValidationResult, RulesService rulesService, boolean isTextContent, boolean isPreHeader){
        List<ValidationResult> validationResults = new ArrayList<>();
        if (contentTobeValidated == null || contentTobeValidated.length() == 0)
            return validationResults;

        ValidationResult objFound;
        boolean found = false;
        contentTobeValidated = getSanitizedBodyFromPreheader(contentTobeValidated);
        rulesService.Validate(contentTobeValidated);

        List<ValidationResult> violatedRules = rulesService.getRulesValidationResults();
        if (violatedRules != null && violatedRules.size() > 0) {
            objFound = new ValidationResult();
            List<Pair<Integer, Integer>> foundLocation = new ArrayList<>();
            for (ValidationResult validatedResult : violatedRules) {
                if (validatedResult.getResultDescription() == INFO_SPAMASSASSINRULES_PLACEHOLDER_ERROR) {
                    foundLocation.add(new ImmutablePair<>(validatedResult.getResultLocationStart(), validatedResult.getResultLocationEnd()));
                    if (!found) {
                        objFound = validatedResult;
                        found = true;
                        continue;
                    }
                    objFound.setResultLocationEnd(validatedResult.getResultLocationEnd());
                }
            }

            // For more convenient way to deliver the information of locations and rules violated.
            var consolidatedRulesViolated = getConsolidatedMetadataFromViolatedRules(violatedRules);
            // since we are consolidating, remove these properties
            objFound.setResultLocationStart(null);
            objFound.setResultLocationEnd(null);

            // get the values right
            objFound.setFoundLocations(consolidatedRulesViolated.getLeft());
            objFound.setViolatedRules(consolidatedRulesViolated.getRight());
            objFound.setResultValidationTitle(isTextContent ? BODY_TEXTSPAMWORDS_CHECKER : (isPreHeader ? BODY_HTMLPREHEADERSPAMWORDS_CHECKER : BODY_HTMLSPAMWORDS_CHECKER));
            objFound.setResultType(foundLocation.size() == 0 ? SUCCESS : WARNING);
            objFound.setResultDescription(foundLocation.size() != 0 ? campaignValidationResult : (isPreHeader ?  INFO_PREHEADERS_SUCCESS : INFO_BODY_LINEWORDING_SUCCESS));
            // Set details and documentation Link enum
            objFound.setResultDetails(getConsolidatedDetailsOfViolatedRules(violatedRules));
            objFound.setReferenceDocumentationLink(objFound.getResultType()!=WARNING ? null : String.valueOf(DOCU_LINEWORDING_BESTPRACTICE));
            validationResults.add(objFound);

            //region [ Old ways to extract content ]
            /* String partialContent = UtilityService.getTokenizedString(foundLocation.size() == 0
                            ? null : contentTobeValidated, foundLocation, DEFAULT_TOKEN_OPENING, DEFAULT_TOKEN_CLOSING,
                    null);
             */
            //endregion
        }

        return validationResults;
    }
