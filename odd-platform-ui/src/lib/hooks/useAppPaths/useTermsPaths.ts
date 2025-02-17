import type { Term } from 'generated-sources';
import { useIsEmbeddedPath } from './useIsEmbeddedPath';
import { TermsRoutes } from './shared';

type TermId = Term['id'] | string;

export const useTermsPaths = () => {
  const { updatePath } = useIsEmbeddedPath();

  const baseTermSearchPath = () => `${TermsRoutes.termSearch}`;
  const termSearchPath = (termSearchId: string = TermsRoutes.termSearchIdParam) =>
    updatePath(`${baseTermSearchPath()}/${termSearchId}`);

  const baseTermDetailsPath = () => updatePath(`${TermsRoutes.terms}`);
  const termDetailsPath = (
    termId: TermId = TermsRoutes.termIdParam,
    viewType: string = TermsRoutes.termsViewTypeParam
  ) => `${baseTermDetailsPath()}/${termId}/${viewType}`;

  const termDetailsLinkedEntitiesPath = (termId: TermId = TermsRoutes.termIdParam) =>
    `${termDetailsPath(termId, TermsRoutes.linkedEntities)}`;

  const termDetailsLinkedColumnsPath = (termId: TermId = TermsRoutes.termIdParam) =>
    `${termDetailsPath(termId, TermsRoutes.linkedColumns)}`;

  const termDetailsOverviewPath = (termId: TermId = TermsRoutes.termIdParam) =>
    `${termDetailsPath(termId, TermsRoutes.overview)}`;

  return {
    termSearchPath,
    termDetailsPath,
    termDetailsLinkedEntitiesPath,
    termDetailsLinkedColumnsPath,
    termDetailsOverviewPath,
  };
};
