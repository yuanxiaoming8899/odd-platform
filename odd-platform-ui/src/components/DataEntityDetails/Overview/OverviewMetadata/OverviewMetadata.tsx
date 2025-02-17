import React from 'react';
import { Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { AddIcon, ChevronIcon } from 'components/shared/icons';
import { Button } from 'components/shared/elements';
import {
  getDataEntityCustomMetadataList,
  getDataEntityPredefinedMetadataList,
  getIsEntityStatusDeleted,
} from 'redux/selectors';
import { useAppParams, useCollapse } from 'lib/hooks';
import { useAppSelector } from 'redux/lib/hooks';
import { WithPermissions } from 'components/shared/contexts';
import { Permission } from 'generated-sources';
import MetadataCreateForm from '../../Metadata/MetadataCreateForm/MetadataCreateForm';
import MetadataItem from './MetadataItem/MetadataItem';
import * as S from './OverviewMetadataStyles';

const OverviewMetadata: React.FC = () => {
  const { t } = useTranslation();
  const { dataEntityId } = useAppParams();
  const { contentRef, containerStyle, toggleCollapse, isCollapsed, controlsStyle } =
    useCollapse({ initialMaxHeight: 200 });

  const predefinedMetadata = useAppSelector(
    getDataEntityPredefinedMetadataList(dataEntityId)
  );
  const customMetadata = useAppSelector(getDataEntityCustomMetadataList(dataEntityId));
  const isStatusDeleted = useAppSelector(getIsEntityStatusDeleted(dataEntityId));

  return (
    <>
      <div ref={contentRef} style={containerStyle}>
        <Grid container>
          <Grid container>
            <Grid item xs={12}>
              <S.SubtitleContainer>
                <Typography variant='h2'>{t('Metadata')}</Typography>
                <WithPermissions
                  permissionTo={Permission.DATA_ENTITY_CUSTOM_METADATA_CREATE}
                >
                  {!isStatusDeleted && (
                    <MetadataCreateForm
                      dataEntityId={dataEntityId}
                      btnCreateEl={
                        <Button
                          text={t('Add metadata')}
                          data-qa='add_metadata'
                          buttonType='secondary-lg'
                          startIcon={<AddIcon />}
                        />
                      }
                    />
                  )}
                </WithPermissions>
              </S.SubtitleContainer>
            </Grid>
            {customMetadata.length ? (
              customMetadata.map(item => (
                <MetadataItem
                  dataEntityId={dataEntityId}
                  metadataItem={item}
                  key={item.field.id}
                />
              ))
            ) : (
              <Grid
                item
                xs={12}
                container
                alignItems='center'
                justifyContent='flex-start'
                wrap='nowrap'
              >
                <Typography variant='subtitle2'>{t('Not created')}</Typography>
                <WithPermissions
                  permissionTo={Permission.DATA_ENTITY_CUSTOM_METADATA_CREATE}
                >
                  {!isStatusDeleted && (
                    <MetadataCreateForm
                      dataEntityId={dataEntityId}
                      btnCreateEl={
                        <Button
                          text={t('Add Metadata')}
                          sx={{ ml: 0.5 }}
                          buttonType='tertiary-sm'
                        />
                      }
                    />
                  )}
                </WithPermissions>
              </Grid>
            )}
          </Grid>
        </Grid>
        {predefinedMetadata.length > 0 && (
          <S.PredefinedContainer item container>
            <Grid container>
              {predefinedMetadata.map(item => (
                <MetadataItem
                  dataEntityId={dataEntityId}
                  metadataItem={item}
                  key={item.field.id}
                />
              ))}
            </Grid>
          </S.PredefinedContainer>
        )}
      </div>
      <S.CollapseContainer container style={controlsStyle}>
        <Button
          text={isCollapsed ? t('Show hidden') : t(`Hide`)}
          endIcon={
            <ChevronIcon
              width={10}
              height={5}
              transform={isCollapsed ? 'rotate(0)' : 'rotate(180)'}
            />
          }
          buttonType='service-m'
          onClick={toggleCollapse}
        />
      </S.CollapseContainer>
    </>
  );
};

export default OverviewMetadata;
