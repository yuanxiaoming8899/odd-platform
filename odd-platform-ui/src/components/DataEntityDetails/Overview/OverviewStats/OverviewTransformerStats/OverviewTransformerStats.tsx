import React from 'react';
import { Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { DataEntityClassNameEnum, type DataEntityDetails } from 'generated-sources';
import { UpstreamIcon, DownstreamIcon } from 'components/shared/icons';
import { EntityClassItem, Button, EntitiesListModal } from 'components/shared/elements';
import { useAppPaths } from 'lib/hooks';
import { StatIconContainer } from './OverviewTransformerStatsStyles';

interface OverviewTransformerStatsProps {
  sources: DataEntityDetails['sourceList'];
  targets: DataEntityDetails['targetList'];
  unknownSourcesCount: number;
  unknownTargetsCount: number;
  dataEntityName: string | undefined;
}

const OverviewTransformerStats: React.FC<OverviewTransformerStatsProps> = ({
  sources,
  targets,
  unknownSourcesCount,
  unknownTargetsCount,
  dataEntityName,
}) => {
  const { t } = useTranslation();
  const displayedEntitiesNumber = 10;
  const { dataEntityOverviewPath } = useAppPaths();

  return (
    <Grid container>
      <Grid item xs={12} sx={{ mb: 1.25 }}>
        <EntityClassItem entityClassName={DataEntityClassNameEnum.TRANSFORMER} fullName />
      </Grid>
      <Grid container flexWrap='nowrap' columnGap={1}>
        <Grid item container xs={6} alignItems='flex-start' alignContent='flex-start'>
          <Grid item container xs={12} alignItems='baseline'>
            <StatIconContainer sx={{ mr: 1 }}>
              <UpstreamIcon />
            </StatIconContainer>
            <Typography variant='h2' sx={{ mr: 0.5 }}>
              {(sources?.length || 0) + (unknownSourcesCount || 0)}
            </Typography>
            <Typography variant='h4'>{t('sources')}</Typography>
          </Grid>
          <Grid
            item
            container
            xs={12}
            direction='column'
            alignItems='flex-start'
            sx={{ mt: 1 }}
          >
            {sources
              ?.slice(0, displayedEntitiesNumber)
              .map(source => (
                <Button
                  text={source.internalName || source.externalName}
                  to={dataEntityOverviewPath(source.id)}
                  key={source.id}
                  buttonType='link-m'
                  sx={{ my: 0.25 }}
                />
              ))}
            {unknownSourcesCount ? (
              <Typography variant='subtitle1' sx={{ ml: 0.5 }}>
                {unknownSourcesCount} {t('more source')}
                {unknownSourcesCount === 1 ? '' : 's'} {t('unknown')}
              </Typography>
            ) : null}
            {sources && sources?.length > displayedEntitiesNumber ? (
              <EntitiesListModal
                entities={sources}
                labelFor={t('Sources')}
                dataEntityName={dataEntityName}
                openBtnEl={
                  <Button text={t('Show All')} buttonType='tertiary-m' sx={{ mt: 1 }} />
                }
              />
            ) : null}
          </Grid>
        </Grid>
        <Grid item container xs={6} alignItems='flex-start' alignContent='flex-start'>
          <Grid item container xs={12} alignItems='baseline'>
            <StatIconContainer sx={{ mr: 1 }}>
              <DownstreamIcon />
            </StatIconContainer>
            <Typography variant='h2' sx={{ mr: 0.5 }}>
              {(targets?.length || 0) + (unknownTargetsCount || 0)}
            </Typography>
            <Typography variant='h4'>{t('targets')}</Typography>
          </Grid>
          <Grid
            item
            container
            xs={12}
            direction='column'
            alignItems='flex-start'
            sx={{ mt: 1 }}
          >
            {targets
              ?.slice(0, displayedEntitiesNumber)
              .map(target => (
                <Button
                  text={target.internalName || target.externalName}
                  to={dataEntityOverviewPath(target.id)}
                  key={target.id}
                  sx={{ my: 0.25 }}
                  buttonType='link-m'
                />
              ))}
            {unknownTargetsCount ? (
              <Typography variant='subtitle1' sx={{ ml: 0.5 }}>
                {unknownTargetsCount} {t('more target')}
                {unknownTargetsCount === 1 ? '' : 's'} {t('unknown')}
              </Typography>
            ) : null}
            {targets && targets?.length > displayedEntitiesNumber ? (
              <EntitiesListModal
                entities={targets}
                labelFor={t('Targets')}
                dataEntityName={dataEntityName}
                openBtnEl={
                  <Button text={t('Show All')} buttonType='tertiary-m' sx={{ my: 1 }} />
                }
              />
            ) : null}
          </Grid>
        </Grid>
      </Grid>
    </Grid>
  );
};

export default OverviewTransformerStats;
