import React from 'react';
import { Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { DataEntityClassNameEnum, type DataEntityDetails } from 'generated-sources';
import { Button, EntitiesListModal, EntityClassItem } from 'components/shared/elements';
import { useAppPaths } from 'lib/hooks';

interface OverviewDataInputStatsProps {
  outputs: DataEntityDetails['outputList'];
  unknownOutputsCount: number;
  dataEntityName: string | undefined;
}

const OverviewDataInputStats: React.FC<OverviewDataInputStatsProps> = ({
  outputs,
  unknownOutputsCount,
  dataEntityName,
}) => {
  const { t } = useTranslation();
  const displayedEntitiesNumber = 10;
  const { dataEntityOverviewPath } = useAppPaths();

  return (
    <Grid container>
      <Grid item xs={12} sx={{ ml: 0, mb: 1.25 }}>
        <EntityClassItem entityClassName={DataEntityClassNameEnum.INPUT} fullName />
      </Grid>
      <Grid item container xs={6} alignItems='flex-start' alignContent='flex-start'>
        <Grid item container xs={12} alignItems='baseline'>
          <Typography variant='h2' sx={{ mr: 0.5 }}>
            {(outputs?.length || 0) + (unknownOutputsCount || 0)}
          </Typography>
          <Typography variant='h4'>{t('outputs')}</Typography>
        </Grid>
        <Grid item container xs={12} direction='column' alignItems='flex-start'>
          {outputs
            ?.slice(0, displayedEntitiesNumber)
            .map(output => (
              <Button
                text={output.internalName || output.externalName}
                to={dataEntityOverviewPath(output.id)}
                key={output.id}
                sx={{ my: 0.25 }}
                buttonType='tertiary-m'
              />
            ))}
          {unknownOutputsCount ? (
            <Typography variant='subtitle1' sx={{ ml: 0.5 }}>
              {unknownOutputsCount} {t('more output')}
              {unknownOutputsCount === 1 ? '' : 's'} {t('unknown')}
            </Typography>
          ) : null}
          {outputs && outputs?.length > displayedEntitiesNumber ? (
            <EntitiesListModal
              entities={outputs}
              labelFor={t('Outputs')}
              dataEntityName={dataEntityName}
              openBtnEl={
                <Button text={t('Show All')} buttonType='tertiary-m' sx={{ my: 0.25 }} />
              }
            />
          ) : null}
        </Grid>
      </Grid>
    </Grid>
  );
};

export default OverviewDataInputStats;
